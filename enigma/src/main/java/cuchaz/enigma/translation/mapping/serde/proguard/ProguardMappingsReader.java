package cuchaz.enigma.translation.mapping.serde.proguard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingOperations;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsReader;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class ProguardMappingsReader implements MappingsReader {
	public static final ProguardMappingsReader INSTANCE = new ProguardMappingsReader();
	private static final String NAME = "[a-zA-Z0-9_\\-.$<>]+";
	private static final String TYPE = NAME + "(?:\\[])*";
	private static final String TYPE_LIST = "|(?:(?:" + TYPE + ",)*" + TYPE + ")";
	private static final Pattern CLASS = Pattern.compile("(" + NAME + ") -> (" + NAME + "):");
	private static final Pattern FIELD = Pattern.compile(" {4}(" + TYPE + ") (" + NAME + ") -> (" + NAME + ")");
	private static final Pattern METHOD = Pattern.compile(" {4}(?:[0-9]+:[0-9]+:)?(" + TYPE + ") (" + NAME + ")\\((" + TYPE_LIST + ")\\) -> (" + NAME + ")");

	public ProguardMappingsReader() {
	}

	@Override
	public EntryTree<EntryMapping> read(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws MappingParseException, IOException {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>();

		int lineNumber = 0;
		ClassEntry currentClass = null;

		for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
			lineNumber++;

			if (line.startsWith("#") || line.isEmpty()) {
				continue;
			}

			Matcher classMatcher = CLASS.matcher(line);
			Matcher fieldMatcher = FIELD.matcher(line);
			Matcher methodMatcher = METHOD.matcher(line);

			if (classMatcher.matches()) {
				String name = classMatcher.group(1);
				String targetName = classMatcher.group(2);

				mappings.insert(currentClass = new ClassEntry(name.replace('.', '/')), new EntryMapping(ClassEntry.getInnerName(targetName.replace('.', '/'))));
			} else if (fieldMatcher.matches()) {
				String type = fieldMatcher.group(1);
				String name = fieldMatcher.group(2);
				String targetName = fieldMatcher.group(3);

				if (currentClass == null) {
					throw new MappingParseException(path, lineNumber, "field mapping not inside class: " + line);
				}

				mappings.insert(new FieldEntry(currentClass, name, new TypeDescriptor(getDescriptor(type))), new EntryMapping(targetName));
			} else if (methodMatcher.matches()) {
				String returnType = methodMatcher.group(1);
				String name = methodMatcher.group(2);
				String[] parameterTypes = methodMatcher.group(3).isEmpty() ? new String[0] : methodMatcher.group(3).split(",");
				String targetName = methodMatcher.group(4);

				if (currentClass == null) {
					throw new MappingParseException(path, lineNumber, "method mapping not inside class: " + line);
				}

				mappings.insert(new MethodEntry(currentClass, name, new MethodDescriptor(getDescriptor(returnType, parameterTypes))), new EntryMapping(targetName));
			} else {
				throw new MappingParseException(path, lineNumber, "invalid mapping line: " + line);
			}
		}

		return MappingOperations.invert(mappings);
	}

	private String getDescriptor(String type) {
		StringBuilder descriptor = new StringBuilder();

		while (type.endsWith("[]")) {
			descriptor.append("[");
			type = type.substring(0, type.length() - 2);
		}

		switch (type) {
		case "byte":
			return descriptor + "B";
		case "char":
			return descriptor + "C";
		case "short":
			return descriptor + "S";
		case "int":
			return descriptor + "I";
		case "long":
			return descriptor + "J";
		case "float":
			return descriptor + "F";
		case "double":
			return descriptor + "D";
		case "boolean":
			return descriptor + "Z";
		case "void":
			return descriptor + "V";
		}

		descriptor.append("L");
		descriptor.append(type.replace('.', '/'));
		descriptor.append(";");

		return descriptor.toString();
	}

	private String getDescriptor(String returnType, String[] parameterTypes) {
		StringBuilder descriptor = new StringBuilder();
		descriptor.append('(');

		for (String parameterType : parameterTypes) {
			descriptor.append(getDescriptor(parameterType));
		}

		descriptor.append(')');
		descriptor.append(getDescriptor(returnType));

		return descriptor.toString();
	}
}
