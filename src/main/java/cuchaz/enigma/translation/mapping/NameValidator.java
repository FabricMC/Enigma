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

package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class NameValidator {
	private static final Pattern IDENTIFIER_PATTERN;
	private static final Pattern CLASS_PATTERN;
	private static final List<String> ILLEGAL_IDENTIFIERS = Arrays.asList(
			"abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized",
			"boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte",
			"else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch",
			"extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally",
			"long", "strictfp", "volatile", "const", "float", "native", "super", "while", "_"
	);

	static {
		String identifierRegex = "[A-Za-z_<][A-Za-z0-9_>]*";
		IDENTIFIER_PATTERN = Pattern.compile(identifierRegex);
		CLASS_PATTERN = Pattern.compile(String.format("^(%s(\\.|/))*(%s)$", identifierRegex, identifierRegex));
	}

	public static void validateClassName(String name) {
		if (!CLASS_PATTERN.matcher(name).matches() || ILLEGAL_IDENTIFIERS.contains(name)) {
			throw new IllegalNameException(name, "This doesn't look like a legal class name");
		}
	}

	public static void validateIdentifier(String name) {
		if (!IDENTIFIER_PATTERN.matcher(name).matches() || ILLEGAL_IDENTIFIERS.contains(name)) {
			throw new IllegalNameException(name, "This doesn't look like a legal identifier");
		}
	}

	public static boolean isReserved(String name) {
		return ILLEGAL_IDENTIFIERS.contains(name);
	}
}
