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
package cuchaz.enigma.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javassist.bytecode.Descriptor;

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
		
		// java allows all kinds of weird characters...
		StringBuilder startChars = new StringBuilder();
		StringBuilder partChars = new StringBuilder();
		for (int i = Character.MIN_CODE_POINT; i <= Character.MAX_CODE_POINT; i++) {
			if (Character.isJavaIdentifierStart(i)) {
				startChars.appendCodePoint(i);
			}
			if (Character.isJavaIdentifierPart(i)) {
				partChars.appendCodePoint(i);
			}
		}
		
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
		if (packageRequired && new ClassEntry(name).getPackageName() == null) {
			throw new IllegalNameException(name, "Class must be in a package");
		}
		return Descriptor.toJvmName(name);
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
}
