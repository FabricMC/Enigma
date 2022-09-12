package cuchaz.enigma.translation.mapping.serde.recaf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
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

public class RecafMappingsReader implements MappingsReader {
	public static final RecafMappingsReader INSTANCE = new RecafMappingsReader();
	private static final Pattern METHOD_PATTERN = Pattern.compile("(.*?)\\.(.*?)(\\(.*?) (.*)");
	private static final Pattern FIELD_PATTERN = Pattern.compile("(.*?)\\.(.*?) (.*?) (.*)");
	private static final Pattern CLASS_PATTERN = Pattern.compile("(.*?) (.*)");

	@Override
	public EntryTree<EntryMapping> read(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws MappingParseException, IOException {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>();
		List<String> lines = Files.readAllLines(path);

		for (String line : lines) {
			Matcher methodMatcher = METHOD_PATTERN.matcher(line);

			if (methodMatcher.find()) {
				ClassEntry owner = new ClassEntry(methodMatcher.group(1));
				String name = methodMatcher.group(2);
				MethodDescriptor desc = new MethodDescriptor(methodMatcher.group(3));
				mappings.insert(new MethodEntry(owner, name, desc), new EntryMapping(methodMatcher.group(4)));
				continue;
			}

			Matcher fieldMatcher = FIELD_PATTERN.matcher(line);

			if (fieldMatcher.find()) {
				ClassEntry owner = new ClassEntry(fieldMatcher.group(1));
				String name = fieldMatcher.group(2);
				TypeDescriptor desc = new TypeDescriptor(fieldMatcher.group(3));
				mappings.insert(new FieldEntry(owner, name, desc), new EntryMapping(fieldMatcher.group(4)));
				continue;
			}

			Matcher classMatcher = CLASS_PATTERN.matcher(line);

			if (classMatcher.find()) {
				mappings.insert(new ClassEntry(classMatcher.group(1)), new EntryMapping(classMatcher.group(2)));
			}
		}

		return mappings;
	}
}
