package cuchaz.enigma.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import cuchaz.enigma.mapping.ClassEntry;


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
}
