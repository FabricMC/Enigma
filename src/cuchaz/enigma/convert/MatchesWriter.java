/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
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

	public static <T extends Entry> void writeMembers(MemberMatches<T> matches, File file)
	throws IOException {
		try (FileWriter out = new FileWriter(file)) {
			for (Map.Entry<T,T> match : matches.matches().entrySet()) {
				writeMemberMatch(out, match.getKey(), match.getValue());
			}
			for (T entry : matches.getUnmatchedSourceEntries()) {
				writeMemberMatch(out, entry, null);
			}
			for (T entry : matches.getUnmatchedDestEntries()) {
				writeMemberMatch(out, null, entry);
			}
			for (T entry : matches.getUnmatchableSourceEntries()) {
				writeUnmatchableEntry(out, entry);
			}
		}
	}

	private static <T extends Entry> void writeMemberMatch(FileWriter out, T source, T dest)
	throws IOException {
		if (source != null) {
			writeEntry(out, source);
		}
		out.write(":");
		if (dest != null) {
			writeEntry(out, dest);
		}
		out.write("\n");
	}
	
	private static <T extends Entry> void writeUnmatchableEntry(FileWriter out, T entry)
	throws IOException {
		out.write("!");
		writeEntry(out, entry);
		out.write("\n");
	}
	
	private static <T extends Entry> void writeEntry(FileWriter out, T entry)
	throws IOException {
		if (entry instanceof FieldEntry) {
			writeField(out, (FieldEntry)entry);
		} else if (entry instanceof BehaviorEntry) {
			writeBehavior(out, (BehaviorEntry)entry);
		}
	}
	
	private static void writeField(FileWriter out, FieldEntry fieldEntry)
	throws IOException {
		out.write(fieldEntry.getClassName());
		out.write(" ");
		out.write(fieldEntry.getName());
		out.write(" ");
		out.write(fieldEntry.getType().toString());
	}
	
	private static void writeBehavior(FileWriter out, BehaviorEntry behaviorEntry)
	throws IOException {
		out.write(behaviorEntry.getClassName());
		out.write(" ");
		out.write(behaviorEntry.getName());
		out.write(" ");
		if (behaviorEntry.getSignature() != null) {
			out.write(behaviorEntry.getSignature().toString());
		}
	}
}
