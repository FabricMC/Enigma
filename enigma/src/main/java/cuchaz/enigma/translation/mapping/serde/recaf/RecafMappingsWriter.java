package cuchaz.enigma.translation.mapping.serde.recaf;

import com.google.common.collect.Lists;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class RecafMappingsWriter implements MappingsWriter {

	public static final RecafMappingsWriter INSTANCE = new RecafMappingsWriter();

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
		try {
			Files.deleteIfExists(path);
			Files.createFile(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			Lists.newArrayList(mappings)
					.stream()
					.map(EntryTreeNode::getEntry)
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

		EntryMapping mapping = mappings.get(entry);

		try {
			if (mapping != null && mapping.targetName() != null) {
				if (entry instanceof ClassEntry classEntry) {

					writer.write(classEntry.getFullName());
					writer.write(" ");
					writer.write(mapping.targetName());

				} else if (entry instanceof FieldEntry fieldEntry) {

					writer.write(fieldEntry.getFullName());
					writer.write(" ");
					writer.write(fieldEntry.getDesc().toString());
					writer.write(" ");
					writer.write(mapping.targetName());

				} else if (entry instanceof MethodEntry methodEntry) {

					writer.write(methodEntry.getFullName());
					writer.write(methodEntry.getDesc().toString());
					writer.write(" ");
					writer.write(mapping.targetName());

				}
				writer.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		node.getChildren().forEach(child -> writeEntry(writer, mappings, child));
	}
}
