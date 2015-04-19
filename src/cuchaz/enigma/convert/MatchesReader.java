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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryFactory;
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

	public static <T extends Entry> MemberMatches<T> readMembers(File file)
	throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			MemberMatches<T> matches = new MemberMatches<T>();
			String line = null;
			while ((line = in.readLine()) != null) {
				readMemberMatch(matches, line);
			}
			return matches;
		}
	}

	private static <T extends Entry> void readMemberMatch(MemberMatches<T> matches, String line) {
		if (line.startsWith("!")) {
			T source = readEntry(line.substring(1));
			matches.addUnmatchableSourceEntry(source);
		} else {
			String[] parts = line.split(":", 2);
			T source = readEntry(parts[0]);
			T dest = readEntry(parts[1]);
			if (source != null && dest != null) {
				matches.addMatch(source, dest);
			} else if (source != null) {
				matches.addUnmatchedSourceEntry(source);
			} else if (dest != null) {
				matches.addUnmatchedDestEntry(dest);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Entry> T readEntry(String in) {
		if (in.length() <= 0) {
			return null;
		}
		String[] parts = in.split(" ");
		if (parts.length == 3 && parts[2].indexOf('(') < 0) {
			return (T)new FieldEntry(
				new ClassEntry(parts[0]),
				parts[1],
				new Type(parts[2])
			);
		} else {
			assert(parts.length == 2 || parts.length == 3);
			if (parts.length == 2) {
				return (T)EntryFactory.getBehaviorEntry(parts[0], parts[1]);
			} else if (parts.length == 3) {
				return (T)EntryFactory.getBehaviorEntry(parts[0], parts[1], parts[2]);
			} else {
				throw new Error("Malformed behavior entry: " + in);
			}
		}
	}
}
