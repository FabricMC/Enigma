package cuchaz.enigma.translation.mapping.serde;

import com.google.common.base.Strings;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.LFPrintWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class TinyV2Writer implements MappingsWriter {

	private static final String MINOR_VERSION = "0";
	private final String obfHeader;
	private final String deobfHeader;

	public TinyV2Writer(String obfHeader, String deobfHeader) {
		this.obfHeader = obfHeader;
		this.deobfHeader = deobfHeader;
	}

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters parameters) {
		List<EntryTreeNode<EntryMapping>> classes = StreamSupport.stream(mappings.spliterator(), false).filter(node -> node.getEntry() instanceof ClassEntry).collect(Collectors.toList());

		try (PrintWriter writer = new LFPrintWriter(Files.newBufferedWriter(path))) {
			writer.println("tiny\t2\t" + MINOR_VERSION + "\t" + obfHeader + "\t" + deobfHeader);

			// no escape names

			for (EntryTreeNode<EntryMapping> node : classes) {
				writeClass(writer, node, mappings);
			}
		} catch (IOException ex) {
			ex.printStackTrace(); // TODO add some better logging system
		}
	}

	private void writeClass(PrintWriter writer, EntryTreeNode<EntryMapping> node, EntryMap<EntryMapping> tree) {
		writer.print("c\t");
		ClassEntry classEntry = (ClassEntry) node.getEntry();
		String fullName = classEntry.getFullName();
		writer.print(fullName);
		Deque<String> parts = new LinkedList<>();
		do {
			EntryMapping mapping = tree.get(classEntry);
			if (mapping != null && mapping.getTargetName() != null) {
				parts.addFirst(mapping.getTargetName());
			} else {
				parts.addFirst(classEntry.getName());
			}
			classEntry = classEntry.getOuterClass();
		} while (classEntry != null);

		String mappedName = String.join("$", parts);

		writer.print("\t");

		writer.print(mappedName); // todo escaping when we have v2 fixed later

		writer.println();

		writeComment(writer, node.getValue(), 1);

		for (EntryTreeNode<EntryMapping> child : node.getChildNodes()) {
			Entry entry = child.getEntry();
			if (entry instanceof FieldEntry) {
				writeField(writer, child);
			} else if (entry instanceof MethodEntry) {
				writeMethod(writer, child);
			}
		}
	}

	private void writeMethod(PrintWriter writer, EntryTreeNode<EntryMapping> node) {
		writer.print(indent(1));
		writer.print("m\t");
		writer.print(((MethodEntry) node.getEntry()).getDesc().toString());
		writer.print("\t");
		writer.print(node.getEntry().getName());
		writer.print("\t");
		EntryMapping mapping = node.getValue();
		if (mapping == null || mapping.getTargetName() == null) {
			writer.println(node.getEntry().getName()); // todo fix v2 name inference
		} else {
			writer.println(mapping.getTargetName());

			writeComment(writer, mapping, 2);
		}

		for (EntryTreeNode<EntryMapping> child : node.getChildNodes()) {
			Entry entry = child.getEntry();
			if (entry instanceof LocalVariableEntry) {
				writeParameter(writer, child);
			}
			// TODO write actual local variables
		}
	}

	private void writeField(PrintWriter writer, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() == null)
			return; // Shortcut

		writer.print(indent(1));
		writer.print("f\t");
		writer.print(((FieldEntry) node.getEntry()).getDesc().toString());
		writer.print("\t");
		writer.print(node.getEntry().getName());
		writer.print("\t");
		EntryMapping mapping = node.getValue();
		if (mapping == null || mapping.getTargetName() == null) {
			writer.println(node.getEntry().getName()); // todo fix v2 name inference
		} else {
			writer.println(mapping.getTargetName());

			writeComment(writer, mapping, 2);
		}
	}

	private void writeParameter(PrintWriter writer, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() == null)
			return; // Shortcut

		writer.print(indent(2));
		writer.print("p\t");
		writer.print(((LocalVariableEntry) node.getEntry()).getIndex());
		writer.print("\t");
		writer.print(node.getEntry().getName());
		writer.print("\t");
		EntryMapping mapping = node.getValue();
		if (mapping == null) {
			writer.println(); // todo ???
		} else {
			writer.println(mapping.getTargetName());

			writeComment(writer, mapping, 3);
		}
	}

	private void writeComment(PrintWriter writer, EntryMapping mapping, int indent) {
		if (mapping != null && mapping.getJavadoc() != null) {
			writer.print(indent(indent));
			writer.print("c\t");
			writer.print(MappingHelper.escape(mapping.getJavadoc()));
			writer.println();
		}
	}

	private String indent(int level) {
		return Strings.repeat("\t", level);
	}
}
