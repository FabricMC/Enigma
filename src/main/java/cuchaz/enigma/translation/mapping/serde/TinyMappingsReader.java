package cuchaz.enigma.translation.mapping.serde;

import com.google.common.base.Charsets;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingPair;
import cuchaz.enigma.translation.mapping.tree.HashMappingTree;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public enum TinyMappingsReader implements MappingsReader {
	INSTANCE;

	@Override
	public MappingTree<EntryMapping> read(Path path) throws IOException, MappingParseException {
		return read(path, Files.readAllLines(path, Charsets.UTF_8));
	}

	private MappingTree<EntryMapping> read(Path path, List<String> lines) throws MappingParseException {
		MappingTree<EntryMapping> mappings = new HashMappingTree<>();
		lines.remove(0);

		for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			String line = lines.get(lineNumber);

			try {
				MappingPair<?, EntryMapping> mapping = parseLine(line);
				mappings.insert(mapping.getEntry(), mapping.getMapping());
			} catch (Throwable t) {
				t.printStackTrace();
				throw new MappingParseException(path::toString, lineNumber, t.toString());
			}
		}

		return mappings;
	}

	private MappingPair<?, EntryMapping> parseLine(String line) {
		String[] tokens = line.split("\t");

		String key = tokens[0];
		switch (key) {
			case "CLASS":
				return parseClass(tokens);
			case "FIELD":
				return parseField(tokens);
			case "METHOD":
				return parseMethod(tokens);
			case "MTH-ARG":
				return parseArgument(tokens);
			default:
				throw new RuntimeException("Unknown token '" + key + "'!");
		}
	}

	private MappingPair<ClassEntry, EntryMapping> parseClass(String[] tokens) {
		ClassEntry obfuscatedEntry = new ClassEntry(tokens[1]);
		String mapping = tokens[2];
		return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
	}

	private MappingPair<FieldEntry, EntryMapping> parseField(String[] tokens) {
		ClassEntry ownerClass = new ClassEntry(tokens[1]);
		TypeDescriptor descriptor = new TypeDescriptor(tokens[2]);

		FieldEntry obfuscatedEntry = new FieldEntry(ownerClass, tokens[3], descriptor);
		String mapping = tokens[4];
		return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
	}

	private MappingPair<MethodEntry, EntryMapping> parseMethod(String[] tokens) {
		ClassEntry ownerClass = new ClassEntry(tokens[1]);
		MethodDescriptor descriptor = new MethodDescriptor(tokens[2]);

		MethodEntry obfuscatedEntry = new MethodEntry(ownerClass, tokens[3], descriptor);
		String mapping = tokens[4];
		return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
	}

	private MappingPair<LocalVariableEntry, EntryMapping> parseArgument(String[] tokens) {
		ClassEntry ownerClass = new ClassEntry(tokens[1]);
		MethodDescriptor ownerDescriptor = new MethodDescriptor(tokens[2]);
		MethodEntry ownerMethod = new MethodEntry(ownerClass, tokens[3], ownerDescriptor);
		int variableIndex = Integer.parseInt(tokens[4]);

		String mapping = tokens[5];
		LocalVariableEntry obfuscatedEntry = new LocalVariableEntry(ownerMethod, variableIndex, "", true);
		return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
	}
}
