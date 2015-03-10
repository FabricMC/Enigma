package cuchaz.enigma.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;


public class MatchesWriter {
	
	public static void writeClasses(ClassMatches matches, File file)
	throws IOException {
		try (FileWriter out = new FileWriter(file)) {
			for (ClassMatch match : matches) {
				writeClassMatch(out, match);
			}
		}
	}

	private static void writeClassMatch(FileWriter out, ClassMatch match)
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

	public static void writeFields(FieldMatches fieldMatches, File file)
	throws IOException {
		try (FileWriter out = new FileWriter(file)) {
			for (Map.Entry<FieldEntry,FieldEntry> match : fieldMatches.matches().entrySet()) {
				writeFieldMatch(out, match.getKey(), match.getValue());
			}
			for (FieldEntry fieldEntry : fieldMatches.getUnmatchedSourceFields()) {
				writeFieldMatch(out, fieldEntry, null);
			}
			for (FieldEntry fieldEntry : fieldMatches.getUnmatchedDestFields()) {
				writeFieldMatch(out, null, fieldEntry);
			}
			for (FieldEntry fieldEntry : fieldMatches.getUnmatchableSourceFields()) {
				writeUnmatchableField(out, fieldEntry);
			}
		}
	}

	private static void writeFieldMatch(FileWriter out, FieldEntry source, FieldEntry dest)
	throws IOException {
		if (source != null) {
			writeField(out, source);
		}
		out.write(":");
		if (dest != null) {
			writeField(out, dest);
		}
		out.write("\n");
	}
	
	private static void writeUnmatchableField(FileWriter out, FieldEntry fieldEntry)
	throws IOException {
		out.write("!");
		writeField(out, fieldEntry);
		out.write("\n");
	}
	
	private static void writeField(FileWriter out, FieldEntry fieldEntry)
	throws IOException {
		out.write(fieldEntry.getClassName());
		out.write(" ");
		out.write(fieldEntry.getName());
		out.write(" ");
		out.write(fieldEntry.getType().toString());
	}
}
