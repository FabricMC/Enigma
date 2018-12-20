package cuchaz.enigma.translation.mapping.serde;

import com.google.common.collect.Lists;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.MappingNode;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public enum SrgMappingsWriter implements MappingsWriter {
	INSTANCE;

	@Override
	public void write(MappingTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path) throws IOException {
		Files.deleteIfExists(path);
		Files.createFile(path);

		List<String> classLines = new ArrayList<>();
		List<String> fieldLines = new ArrayList<>();
		List<String> methodLines = new ArrayList<>();

		Collection<Entry<?>> rootEntries = Lists.newArrayList(mappings).stream()
				.map(MappingNode::getEntry)
				.collect(Collectors.toList());
		for (Entry<?> entry : sorted(rootEntries)) {
			writeEntry(classLines, fieldLines, methodLines, mappings, entry);
		}

		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
			classLines.forEach(writer::println);
			fieldLines.forEach(writer::println);
			methodLines.forEach(writer::println);
		}
	}

	private void writeEntry(List<String> classes, List<String> fields, List<String> methods, MappingTree<EntryMapping> mappings, Entry<?> entry) {
		MappingNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		Translator translator = new MappingTranslator(mappings);
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

	private Collection<Entry<?>> sorted(Iterable<Entry<?>> iterable) {
		ArrayList<Entry<?>> sorted = Lists.newArrayList(iterable);
		sorted.sort(Comparator.comparing(Entry::getName));
		return sorted;
	}
}
