package cuchaz.enigma.translation.mapping.serde;

import com.google.common.base.Charsets;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingPair;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;

final class TinyV2Reader implements MappingsReader {

	private static final String MINOR_VERSION = "0";
	// 0 indent
	private static final int IN_HEADER = 0;
	private static final int IN_CLASS = IN_HEADER + 1;
	// 1 indent
	private static final int IN_METHOD = IN_CLASS + 1;
	private static final int IN_FIELD = IN_METHOD + 1;
	// 2 indent
	private static final int IN_PARAMETER = IN_FIELD + 1;
	// general properties
	private static final int STATE_SIZE = IN_PARAMETER + 1;
	private static final int[] INDENT_CLEAR_START = {IN_HEADER, IN_METHOD, IN_PARAMETER, STATE_SIZE};

	@Override
	public EntryTree<EntryMapping> read(Path path, ProgressListener progress) throws IOException, MappingParseException {
		return read(path, Files.readAllLines(path, Charsets.UTF_8), progress);
	}

	private EntryTree<EntryMapping> read(Path path, List<String> lines, ProgressListener progress) throws MappingParseException {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>();

		progress.init(lines.size(), "Loading mapping file");

		BitSet state = new BitSet(STATE_SIZE);
		@SuppressWarnings({"unchecked", "rawtypes"})
		MappingPair<? extends Entry<?>, RawEntryMapping>[] holds = new MappingPair[STATE_SIZE];
		boolean escapeNames = false;

		for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			try {
				progress.step(lineNumber, "");
				String line = lines.get(lineNumber);

				int indent = 0;
				while (line.charAt(indent) == '\t')
					indent++;

				String[] parts = line.substring(indent).split("\t");
				if (parts.length == 0 || indent >= INDENT_CLEAR_START.length)
					throw new IllegalArgumentException("Invalid format");

				// clean and register stuff in stack
				for (int i = INDENT_CLEAR_START[indent]; i < STATE_SIZE; i++) {
					state.clear(i);
					if (holds[i] != null) {
						RawEntryMapping mapping = holds[i].getMapping();
						if (mapping != null) {
							mappings.insert(holds[i].getEntry(), mapping.bake());
						}
						holds[i] = null;
					}
				}

				switch (indent) {
					case 0:
						switch (parts[0]) {
							case "tiny": // header
								if (lineNumber != 0) {
									throw new IllegalArgumentException("Header can only be on the first line");
								}
								if (parts.length < 5) {
									throw new IllegalArgumentException("Not enough header columns, needs at least 5");
								}
								if (!"2".equals(parts[1]) || !MINOR_VERSION.equals(parts[2])) {
									throw new IllegalArgumentException("Unsupported TinyV2 version, requires major " + "2" + " and minor " + MINOR_VERSION + "");
								}
								state.set(IN_HEADER);
								break;
							case "c": // class
								state.set(IN_CLASS);
								holds[IN_CLASS] = parseClass(parts);
								break;
							default:
								unsupportKey(parts);
						}

						break;
					case 1:
						if (state.get(IN_HEADER)) {
							if (parts[0].equals("esacpe-names")) {
								escapeNames = true;
							}

							break;
						}

						if (state.get(IN_CLASS)) {
							switch (parts[0]) {
								case "m": // method
									state.set(IN_METHOD);
									holds[IN_METHOD] = parseMethod(parts);
									break;
								case "f": // field
									state.set(IN_FIELD);
									holds[IN_FIELD] = parseField(parts);
									break;
								case "c": // class javadoc
									addJavadoc(holds[IN_CLASS], parts);
									break;
								default:
									unsupportKey(parts);
							}
							break;
						}

						unsupportKey(parts);
					case 2:
						if (state.get(IN_METHOD)) {
							switch (parts[0]) {
								case "p": // parameter
									state.set(IN_PARAMETER);
									holds[IN_PARAMETER] = parseArgument(parts);
									break;
								case "v": // local variable
									// Can't do anything yet
									break;
								case "c": // method javadoc
									addJavadoc(holds[IN_METHOD], parts);
									break;
								default:
									unsupportKey(parts);
							}
							break;
						}

						if (state.get(IN_FIELD)) {
							switch (parts[0]) {
								case "c": // field javadoc
									addJavadoc(holds[IN_METHOD], parts);
									break;
								default:
									unsupportKey(parts);
							}
							break;
						}
						unsupportKey(parts);
					case 3:
						if (state.get(IN_PARAMETER)) {
							switch (parts[0]) {
								case "c":
									addJavadoc(holds[IN_PARAMETER], parts);
									break;
								default:
									unsupportKey(parts);
							}
							break;
						}
						unsupportKey(parts);
					default:
						unsupportKey(parts);
				}

			} catch (Throwable t) {
				t.printStackTrace();
				throw new MappingParseException(path::toString, lineNumber, t.toString());
			}
		}

		return mappings;
	}

	private void unsupportKey(String[] parts) {
		throw new IllegalArgumentException("Unsupported key " + parts[0]);
	}

	private void addJavadoc(MappingPair<? extends Entry, RawEntryMapping> pair, String[] parts) {
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid javadoc declaration");
		}

		addJavadoc(pair, parts[1]);
	}

	private void addJavadoc(MappingPair<? extends Entry, RawEntryMapping> pair, String javadoc) {
		RawEntryMapping mapping = pair.getMapping();
		if (mapping == null) {
			throw new IllegalArgumentException("Javadoc requires a mapping!");
		}
		mapping.addJavadocLine(javadoc);
	}

	private MappingPair<ClassEntry, RawEntryMapping> parseClass(String[] tokens) {
		ClassEntry obfuscatedEntry = new ClassEntry(tokens[1]);
		String mapping = tokens[2];
		return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping));
	}

	private MappingPair<FieldEntry, RawEntryMapping> parseField(String[] tokens) {
		ClassEntry ownerClass = new ClassEntry(tokens[1]);
		TypeDescriptor descriptor = new TypeDescriptor(tokens[2]);

		FieldEntry obfuscatedEntry = new FieldEntry(ownerClass, tokens[3], descriptor);
		String mapping = tokens[4];
		return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping));
	}

	private MappingPair<MethodEntry, RawEntryMapping> parseMethod(String[] tokens) {
		ClassEntry ownerClass = new ClassEntry(tokens[1]);
		MethodDescriptor descriptor = new MethodDescriptor(tokens[2]);

		MethodEntry obfuscatedEntry = new MethodEntry(ownerClass, tokens[3], descriptor);
		String mapping = tokens[4];
		return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping));
	}

	private MappingPair<LocalVariableEntry, RawEntryMapping> parseArgument(String[] tokens) {
		ClassEntry ownerClass = new ClassEntry(tokens[1]);
		MethodDescriptor ownerDescriptor = new MethodDescriptor(tokens[2]);
		MethodEntry ownerMethod = new MethodEntry(ownerClass, tokens[3], ownerDescriptor);
		int variableIndex = Integer.parseInt(tokens[4]);

		String mapping = tokens[5];
		LocalVariableEntry obfuscatedEntry = new LocalVariableEntry(ownerMethod, variableIndex, "", true, null);
		return new MappingPair<>(obfuscatedEntry, new RawEntryMapping(mapping));
	}
}
