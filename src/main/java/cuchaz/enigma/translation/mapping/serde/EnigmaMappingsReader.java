package cuchaz.enigma.translation.mapping.serde;

import com.google.common.base.Charsets;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingPair;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum EnigmaMappingsReader implements MappingsReader {
	FILE {
		@Override
		public EntryTree<EntryMapping> read(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
			progress.init(1, "Loading mapping file");

			EntryTree<EntryMapping> mappings = new HashEntryTree<>();
			readFile(path, mappings);

			progress.step(1, "Done!");

			return mappings;
		}
	},
	DIRECTORY {
		@Override
		public EntryTree<EntryMapping> read(Path root, ProgressListener progress, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
			EntryTree<EntryMapping> mappings = new HashEntryTree<>();

			List<Path> files = Files.walk(root)
					.filter(f -> !Files.isDirectory(f))
					.filter(f -> f.toString().endsWith(".mapping"))
					.collect(Collectors.toList());

			progress.init(files.size(), "Loading mapping files");
			int step = 0;

			for (Path file : files) {
				progress.step(step++, root.relativize(file).toString());
				if (Files.isHidden(file)) {
					continue;
				}
				readFile(file, mappings);
			}

			return mappings;
		}
	};

	protected void readFile(Path path, EntryTree<EntryMapping> mappings) throws IOException, MappingParseException {
		List<String> lines = Files.readAllLines(path, Charsets.UTF_8);
		Deque<MappingPair<?, RawEntryMapping>> mappingStack = new ArrayDeque<>();

		for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			String line = lines.get(lineNumber);
			int indentation = countIndentation(line);

			line = formatLine(line);
			if (line == null) {
				continue;
			}

			cleanMappingStack(indentation, mappingStack, mappings);

			try {
				MappingPair<?, RawEntryMapping> pair = parseLine(mappingStack.peek(), line);
				if (pair != null) {
					mappingStack.push(pair);
					if (pair.getMapping() != null) {

					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
				throw new MappingParseException(path::toString, lineNumber, t.toString());
			}
		}

		// Clean up rest
		cleanMappingStack(0, mappingStack, mappings);
	}

	private void cleanMappingStack(int indentation, Deque<MappingPair<?, RawEntryMapping>> mappingStack, EntryTree<EntryMapping> mappings) {
		while (indentation < mappingStack.size()) {
			MappingPair<?, RawEntryMapping> pair = mappingStack.pop();
			if (pair.getMapping() != null) {
				mappings.insert(pair.getEntry(), pair.getMapping().bake());
			}
		}
	}

	@Nullable
	private String formatLine(String line) {
		line = stripComment(line);
		line = line.trim();

		if (line.isEmpty()) {
			return null;
		}

		return line;
	}

	private String stripComment(String line) {
		int commentPos = line.indexOf('#');
		if (commentPos >= 0) {
			return line.substring(0, commentPos);
		}
		return line;
	}

	private int countIndentation(String line) {
		int indent = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) != '\t') {
				break;
			}
			indent++;
		}
		return indent;
	}

	private MappingPair<?, RawEntryMapping> parseLine(@Nullable MappingPair<?, RawEntryMapping> parent, String line) {
		String[] tokens = line.trim().split("\\s");
		String keyToken = tokens[0].toUpperCase(Locale.ROOT);
		Entry<?> parentEntry = parent == null ? null : parent.getEntry();

		switch (keyToken) {
			case EnigmaFormat.CLASS:
				return parseClass(parentEntry, tokens);
			case EnigmaFormat.FIELD:
				return parseField(parentEntry, tokens);
			case EnigmaFormat.METHOD:
				return parseMethod(parentEntry, tokens);
			case EnigmaFormat.PARAMETER:
				return parseArgument(parentEntry, tokens);
			case EnigmaFormat.COMMENT:
				readJavadoc(parent, tokens);
				return null;
			default:
				throw new RuntimeException("Unknown token '" + keyToken + "'");
		}
	}
	
	private void readJavadoc(MappingPair<?, RawEntryMapping> parent, String[] tokens) {
		if (parent == null)
			throw new IllegalStateException("Javadoc has no parent!");
		String jdLine = tokens.length > 1 ? tokens[1] : ""; // Empty string to concat
		if (parent.getMapping() == null)
			throw new IllegalStateException("Javadoc requires a mapping!");
		parent.getMapping().addJavadocLine(MappingHelper.unescape(jdLine));
	}

	private MappingPair<ClassEntry, RawEntryMapping> parseClass(@Nullable Entry<?> parent, String[] tokens) {
		String obfuscatedName = ClassEntry.getInnerName(tokens[1]);
		ClassEntry obfuscatedEntry;
		if (parent instanceof ClassEntry) {
			obfuscatedEntry = new ClassEntry((ClassEntry) parent, obfuscatedName);
		} else {
			obfuscatedEntry = new ClassEntry(obfuscatedName);
		}

		String mapping = null;
		AccessModifier modifier = AccessModifier.UNCHANGED;

		if (tokens.length == 3) {
			AccessModifier parsedModifier = parseModifier(tokens[2]);
			if (parsedModifier != null) {
				modifier = parsedModifier;
				mapping = obfuscatedName;
			} else {
				mapping = tokens[2];
			}
		} else if (tokens.length == 4) {
			mapping = tokens[2];
			modifier = parseModifier(tokens[3]);
		}

		if (mapping != null) {
			return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping, modifier));
		} else {
			return new MappingPair<>(obfuscatedEntry);
		}
	}

	private MappingPair<FieldEntry, RawEntryMapping> parseField(@Nullable Entry<?> parent, String[] tokens) {
		if (!(parent instanceof ClassEntry)) {
			throw new RuntimeException("Field must be a child of a class!");
		}

		ClassEntry ownerEntry = (ClassEntry) parent;

		String obfuscatedName = tokens[1];
		String mapping = obfuscatedName;
		AccessModifier modifier = AccessModifier.UNCHANGED;
		TypeDescriptor descriptor;

		if (tokens.length == 4) {
			AccessModifier parsedModifier = parseModifier(tokens[3]);
			if (parsedModifier != null) {
				descriptor = new TypeDescriptor(tokens[2]);
				modifier = parsedModifier;
			} else {
				mapping = tokens[2];
				descriptor = new TypeDescriptor(tokens[3]);
			}
		} else if (tokens.length == 5) {
			descriptor = new TypeDescriptor(tokens[3]);
			mapping = tokens[2];
			modifier = parseModifier(tokens[4]);
		} else {
			throw new RuntimeException("Invalid field declaration");
		}

		FieldEntry obfuscatedEntry = new FieldEntry(ownerEntry, obfuscatedName, descriptor);
		if (mapping != null) {
			return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping, modifier));
		} else {
			return new MappingPair<>(obfuscatedEntry);
		}
	}

	private MappingPair<MethodEntry, RawEntryMapping> parseMethod(@Nullable Entry<?> parent, String[] tokens) {
		if (!(parent instanceof ClassEntry)) {
			throw new RuntimeException("Method must be a child of a class!");
		}

		ClassEntry ownerEntry = (ClassEntry) parent;

		String obfuscatedName = tokens[1];
		String mapping = null;
		AccessModifier modifier = AccessModifier.UNCHANGED;
		MethodDescriptor descriptor;

		if (tokens.length == 3) {
			descriptor = new MethodDescriptor(tokens[2]);
		} else if (tokens.length == 4) {
			AccessModifier parsedModifier = parseModifier(tokens[3]);
			if (parsedModifier != null) {
				modifier = parsedModifier;
				mapping = obfuscatedName;
				descriptor = new MethodDescriptor(tokens[2]);
			} else {
				mapping = tokens[2];
				descriptor = new MethodDescriptor(tokens[3]);
			}
		} else if (tokens.length == 5) {
			mapping = tokens[2];
			modifier = parseModifier(tokens[4]);
			descriptor = new MethodDescriptor(tokens[3]);
		} else {
			throw new RuntimeException("Invalid method declaration");
		}

		MethodEntry obfuscatedEntry = new MethodEntry(ownerEntry, obfuscatedName, descriptor);
		if (mapping != null) {
			return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping, modifier));
		} else {
			return new MappingPair<>(obfuscatedEntry);
		}
	}

	private MappingPair<LocalVariableEntry, RawEntryMapping> parseArgument(@Nullable Entry<?> parent, String[] tokens) {
		if (!(parent instanceof MethodEntry)) {
			throw new RuntimeException("Method arg must be a child of a method!");
		}

		MethodEntry ownerEntry = (MethodEntry) parent;
		LocalVariableEntry obfuscatedEntry = new LocalVariableEntry(ownerEntry, Integer.parseInt(tokens[1]), "", true, null);
		String mapping = tokens[2];

		return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping));
	}

	@Nullable
	private AccessModifier parseModifier(String token) {
		if (token.startsWith("ACC:")) {
			return AccessModifier.valueOf(token.substring(4));
		}
		return null;
	}
}
