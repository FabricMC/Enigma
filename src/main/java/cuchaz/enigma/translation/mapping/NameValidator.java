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

import cuchaz.enigma.translation.representation.ClassEntry;
import cuchaz.enigma.throwables.IllegalNameException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class NameValidator {

	private static final Pattern IdentifierPattern;
	private static final Pattern ClassPattern;
	private static final List<String> ReservedWords = Arrays.asList(
			"abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized",
			"boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte",
			"else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch",
			"extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally",
			"long", "strictfp", "volatile", "const", "float", "native", "super", "while"
	);

	static {
		String identifierRegex = "[A-Za-z_<][A-Za-z0-9_>]*";
		IdentifierPattern = Pattern.compile(identifierRegex);
		ClassPattern = Pattern.compile(String.format("^(%s(\\.|/))*(%s)$", identifierRegex, identifierRegex));
	}

	public static String validateClassName(String name, boolean packageRequired) {
		if (name == null) {
			return null;
		}
		if (!ClassPattern.matcher(name).matches() || ReservedWords.contains(name)) {
			throw new IllegalNameException(name, "This doesn't look like a legal class name");
		}
		if (packageRequired && ClassEntry.getPackageName(name) == null) {
			throw new IllegalNameException(name, "Class must be in a package");
		}
		return name;
	}

	public static String validateFieldName(String name) {
		if (name == null) {
			return null;
		}
		if (!IdentifierPattern.matcher(name).matches() || ReservedWords.contains(name)) {
			throw new IllegalNameException(name, "This doesn't look like a legal identifier");
		}
		return name;
	}

	public static String validateMethodName(String name) {
		return validateFieldName(name);
	}

	public static String validateArgumentName(String name) {
		return validateFieldName(name);
	}

	public static boolean isReserved(String name) {
		return ReservedWords.contains(name);
	}
}
