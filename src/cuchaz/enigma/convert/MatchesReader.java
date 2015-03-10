package cuchaz.enigma.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Type;


public class MatchesReader {
	
	public static ClassMatches readClasses(File file)
	throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			ClassMatches matches = new ClassMatches();
			String line = null;
			while ((line = in.readLine()) != null) {
				matches.add(readClassMatch(line));
			}
			return matches;
		}
	}

	private static ClassMatch readClassMatch(String line)
	throws IOException {
		String[] sides = line.split(":", 2);
		return new ClassMatch(readClasses(sides[0]), readClasses(sides[1]));
	}

	private static Collection<ClassEntry> readClasses(String in) {
		List<ClassEntry> entries = Lists.newArrayList();
		for (String className : in.split(",")) {
			className = className.trim();
			if (className.length() > 0) {
				entries.add(new ClassEntry(className));
			}
		}
		return entries;
	}

	public static FieldMatches readFields(File file)
	throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			FieldMatches matches = new FieldMatches();
			String line = null;
			while ((line = in.readLine()) != null) {
				readFieldMatch(matches, line);
			}
			return matches;
		}
	}

	private static void readFieldMatch(FieldMatches matches, String line) {
		if (line.startsWith("!")) {
			matches.addUnmatchableSourceField(readField(line.substring(1)));
		} else {
			String[] parts = line.split(":", 2);
			FieldEntry source = readField(parts[0]);
			FieldEntry dest = readField(parts[1]);
			if (source != null && dest != null) {
				matches.addMatch(source, dest);
			} else if (source != null) {
				matches.addUnmatchedSourceField(source);
			} else if (dest != null) {
				matches.addUnmatchedDestField(dest);
			}
		}
	}

	private static FieldEntry readField(String in) {
		if (in.length() <= 0) {
			return null;
		}
		String[] parts = in.split(" ");
		assert(parts.length == 3);
		return new FieldEntry(
			new ClassEntry(parts[0]),
			parts[1],
			new Type(parts[2])
		);
	}
}
