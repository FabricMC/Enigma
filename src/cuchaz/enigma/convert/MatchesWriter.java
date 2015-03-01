package cuchaz.enigma.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import cuchaz.enigma.mapping.ClassEntry;


public class MatchesWriter {
	
	public static void write(Matches matches, File file)
	throws IOException {
		try (FileWriter out = new FileWriter(file)) {
			for (ClassMatch match : matches) {
				writeMatch(out, match);
			}
		}
	}

	private static void writeMatch(FileWriter out, ClassMatch match)
	throws IOException {
		writeClasses(out, match.sourceClasses);
		out.write(":");
		writeClasses(out, match.destClasses);
		out.write("\n");
	}
	
	private static void writeClasses(FileWriter out, Iterable<ClassEntry> classes)
	throws IOException {
		boolean isFirst = true;
		for (ClassEntry entry : classes) {
			if (isFirst) {
				isFirst = false;
			} else {
				out.write(",");
			}
			out.write(entry.toString());
		}
	}
}
