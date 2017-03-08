/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.convert;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;

public class MatchesWriter {

	public static void writeClasses(ClassMatches matches, File file)
		throws IOException {
		try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"))) {
			for (ClassMatch match : matches) {
				writeClassMatch(out, match);
			}
		}
	}

	private static void writeClassMatch(OutputStreamWriter out, ClassMatch match)
		throws IOException {
		writeClasses(out, match.sourceClasses);
		out.write(":");
		writeClasses(out, match.destClasses);
		out.write("\n");
	}

	private static void writeClasses(OutputStreamWriter out, Iterable<ClassEntry> classes)
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
		try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"))) {
			for (Map.Entry<T, T> match : matches.matches().entrySet()) {
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

	private static <T extends Entry> void writeMemberMatch(OutputStreamWriter out, T source, T dest)
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

	private static <T extends Entry> void writeUnmatchableEntry(OutputStreamWriter out, T entry)
		throws IOException {
		out.write("!");
		writeEntry(out, entry);
		out.write("\n");
	}

	private static <T extends Entry> void writeEntry(OutputStreamWriter out, T entry)
		throws IOException {
		if (entry instanceof FieldEntry) {
			writeField(out, (FieldEntry) entry);
		} else if (entry instanceof BehaviorEntry) {
			writeBehavior(out, (BehaviorEntry) entry);
		}
	}

	private static void writeField(OutputStreamWriter out, FieldEntry fieldEntry)
		throws IOException {
		out.write(fieldEntry.getClassName());
		out.write(" ");
		out.write(fieldEntry.getName());
		out.write(" ");
		out.write(fieldEntry.getType().toString());
	}

	private static void writeBehavior(OutputStreamWriter out, BehaviorEntry behaviorEntry)
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
