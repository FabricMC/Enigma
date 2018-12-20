package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum SrgMappingsWriter implements MappingsWriter {
	INSTANCE;

	@Override
	public void write(MappingTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path) throws IOException {
		Files.deleteIfExists(path);
		Files.createFile(path);

		Translator translator = new MappingTranslator(mappings);

		List<String> classLines = new ArrayList<>();
		List<String> fieldLines = new ArrayList<>();
		List<String> methodLines = new ArrayList<>();

		// TODO: sort alphabetically
		Collection<Entry<?>> entries = mappings.getAllEntries();
		for (Entry<?> entry : entries) {
			if (entry instanceof ClassEntry) {
				classLines.add(generateClassLine((ClassEntry) entry, translator));
			} else if (entry instanceof FieldEntry) {
				fieldLines.add(generateFieldLine((FieldEntry) entry, translator));
			} else if (entry instanceof MethodEntry) {
				methodLines.add(generateMethodLine((MethodEntry) entry, translator));
			}
		}

		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
			classLines.forEach(writer::println);
			fieldLines.forEach(writer::println);
			methodLines.forEach(writer::println);
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
}
