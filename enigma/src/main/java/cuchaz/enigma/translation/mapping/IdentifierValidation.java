/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation.mapping;

import java.util.List;

import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;
import cuchaz.enigma.utils.validation.ValidationContext;

public final class IdentifierValidation {
	private IdentifierValidation() {
	}

	private static final List<String> ILLEGAL_IDENTIFIERS = List.of("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements",
																	"import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
																	"transient", "true", "try", "void", "volatile", "while", "_");

	public static boolean validateClassName(ValidationContext vc, String name, boolean isInner) {
		if (!StandardValidation.notBlank(vc, name)) {
			return false;
		}

		if (isInner) {
			// When renaming, inner class names do not contain the package name,
			// but only the class name.
			return validateIdentifier(vc, name);
		}

		String[] parts = name.split("/");

		for (String part : parts) {
			validateIdentifier(vc, part);
		}

		return true;
	}

	public static boolean validateIdentifier(ValidationContext vc, String name) {
		if (!StandardValidation.notBlank(vc, name)) {
			return false;
		}

		if (checkForReservedName(vc, name)) {
			return false;
		}

		// Adapted from javax.lang.model.SourceVersion.isIdentifier

		int cp = name.codePointAt(0);
		int position = 1;

		if (!Character.isJavaIdentifierStart(cp)) {
			vc.raise(Message.ILLEGAL_IDENTIFIER, name, new String(Character.toChars(cp)), position);
			return false;
		}

		for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
			cp = name.codePointAt(i);
			position += 1;

			if (!Character.isJavaIdentifierPart(cp)) {
				vc.raise(Message.ILLEGAL_IDENTIFIER, name, new String(Character.toChars(cp)), position);
				return false;
			}
		}

		return true;
	}

	private static boolean checkForReservedName(ValidationContext vc, String name) {
		if (isReservedMethodName(name)) {
			vc.raise(Message.RESERVED_IDENTIFIER, name);
			return true;
		}

		return false;
	}

	public static boolean isReservedMethodName(String name) {
		return ILLEGAL_IDENTIFIERS.contains(name);
	}
}
