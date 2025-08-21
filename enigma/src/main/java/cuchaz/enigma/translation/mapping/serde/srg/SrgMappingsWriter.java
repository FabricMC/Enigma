package cuchaz.enigma.translation.mapping.serde.srg;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.LfPrintWriter;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public enum SrgMappingsWriter implements MappingsWriter {
	INSTANCE;

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
		try {
			Files.deleteIfExists(path);
			Files.createFile(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> classLines = new ArrayList<>();
		List<String> fieldLines = new ArrayList<>();
		List<String> methodLines = new ArrayList<>();

		List<? extends Entry<?>> rootEntries = new ArrayList<>(mappings).stream().map(EntryTreeNode::getEntry).toList();
		progress.init(rootEntries.size(), I18n.translate("progress.mappings.converting"));

		int steps = 0;

		for (Entry<?> entry : sorted(rootEntries)) {
			progress.step(steps++, entry.getName());
			writeEntry(classLines, fieldLines, methodLines, mappings, entry);
		}

		progress.init(3, I18n.translate("progress.mappings.writing"));

		try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
			progress.step(0, I18n.translate("type.classes"));
			classLines.forEach(writer::println);
			progress.step(1, I18n.translate("type.fields"));
			fieldLines.forEach(writer::println);
			progress.step(2, I18n.translate("type.methods"));
			methodLines.forEach(writer::println);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeEntry(List<String> classes, List<String> fields, List<String> methods, EntryTree<EntryMapping> mappings, Entry<?> entry) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);

		if (node == null) {
			return;
		}

		Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);

		if (entry instanceof ClassEntry) {
			classes.add(generateClassLine((ClassEntry) entry, translator));
		} else if (entry instanceof FieldEntry) {
			fields.add(generateFieldLine((FieldEntry) entry, translator));
		} else if (entry instanceof MethodEntry) {
			methods.add(generateMethodLine((MethodEntry) entry, translator));
		}

		for (Entry<?> child : sorted(node.getChildren())) {
			writeEntry(classes, fields, methods, mappings, child);
		}
	}

	private String generateClassLine(ClassEntry sourceEntry, Translator translator) {
		ClassEntry targetEntry = translator.translate(sourceEntry);
		return "CL: " + sourceEntry.getFullName() + " " + targetEntry.getFullName();
	}

	private String generateMethodLine(MethodEntry sourceEntry, Translator translator) {
		MethodEntry targetEntry = translator.translate(sourceEntry);
		return "MD: " + describeMethod(sourceEntry) + " " + describeMethod(targetEntry);
	}

	private String describeMethod(MethodEntry entry) {
		return entry.getParent().getFullName() + "/" + entry.getName() + " " + entry.getDesc();
	}

	private String generateFieldLine(FieldEntry sourceEntry, Translator translator) {
		FieldEntry targetEntry = translator.translate(sourceEntry);
		return "FD: " + describeField(sourceEntry) + " " + describeField(targetEntry);
	}

	private String describeField(FieldEntry entry) {
		return entry.getParent().getFullName() + "/" + entry.getName();
	}

	private Collection<Entry<?>> sorted(Collection<? extends Entry<?>> collection) {
		ArrayList<Entry<?>> sorted = new ArrayList<>(collection);
		sorted.sort(Comparator.comparing(Entry::getName));
		return sorted;
	}
}
