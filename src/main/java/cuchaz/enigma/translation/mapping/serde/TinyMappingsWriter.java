package cuchaz.enigma.translation.mapping.serde;

import com.google.common.collect.Lists;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class TinyMappingsWriter implements MappingsWriter {
	private static final String VERSION_CONSTANT = "v1";

	// HACK: as of enigma 0.13.1, some fields seem to appear duplicated?
	private final Set<String> writtenLines = new HashSet<>();
	private final String nameObf;
	private final String nameDeobf;

	public TinyMappingsWriter(String nameObf, String nameDeobf) {
		this.nameObf = nameObf;
		this.nameDeobf = nameDeobf;
	}

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress) {
		try {
			Files.deleteIfExists(path);
			Files.createFile(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writeLine(writer, new String[]{VERSION_CONSTANT, nameObf, nameDeobf});

			Lists.newArrayList(mappings).stream()
					.map(EntryTreeNode::getEntry).sorted(Comparator.comparing(Object::toString))
					.forEach(entry -> writeEntry(writer, mappings, entry));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeEntry(Writer writer, EntryTree<EntryMapping> mappings, Entry<?> entry) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);

		EntryMapping mapping = mappings.get(entry);
		if (mapping != null && !entry.getName().equals(mapping.getTargetName())) {
			if (entry instanceof ClassEntry) {
				writeClass(writer, (ClassEntry) entry, translator);
			} else if (entry instanceof FieldEntry) {
				writeLine(writer, TinyV1Helper.serializeEntry(entry, true, mapping.getTargetName()));
			} else if (entry instanceof MethodEntry) {
				writeLine(writer, TinyV1Helper.serializeEntry(entry, true, mapping.getTargetName()));
			}
		}

		writeChildren(writer, mappings, node);
	}

	private void writeChildren(Writer writer, EntryTree<EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
		node.getChildren().stream()
				.filter(e -> e instanceof FieldEntry).sorted()
				.forEach(child -> writeEntry(writer, mappings, child));

		node.getChildren().stream()
				.filter(e -> e instanceof MethodEntry).sorted()
				.forEach(child -> writeEntry(writer, mappings, child));

		node.getChildren().stream()
				.filter(e -> e instanceof ClassEntry).sorted()
				.forEach(child -> writeEntry(writer, mappings, child));
	}

	private void writeClass(Writer writer, ClassEntry entry, Translator translator) {
		ClassEntry translatedEntry = translator.translate(entry);

		String obfClassName = cuchaz.enigma.utils.Utils.NONE_PREFIX_REMOVER.map(entry.getFullName());
		String deobfClassName = cuchaz.enigma.utils.Utils.NONE_PREFIX_REMOVER.map(translatedEntry.getFullName());
		writeLine(writer, new String[]{"CLASS", obfClassName, deobfClassName});
	}

	private void writeLine(Writer writer, String[] data) {
		try {
			String line = Utils.TAB_JOINER.join(data) + "\n";
			if (writtenLines.add(line)) {
				writer.write(line);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
