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

package cuchaz.enigma.analysis;

import java.lang.reflect.Modifier;

import cuchaz.enigma.translation.representation.AccessFlags;

public enum Access {
	PUBLIC,
	PROTECTED,
	PACKAGE,
	PRIVATE;

	public static Access get(AccessFlags flags) {
		return get(flags.getFlags());
	}

	public static Access get(int modifiers) {
		boolean isPublic = Modifier.isPublic(modifiers);
		boolean isProtected = Modifier.isProtected(modifiers);
		boolean isPrivate = Modifier.isPrivate(modifiers);

		if (isPublic && !isProtected && !isPrivate) {
			return PUBLIC;
		} else if (!isPublic && isProtected && !isPrivate) {
			return PROTECTED;
		} else if (!isPublic && !isProtected && isPrivate) {
			return PRIVATE;
		} else if (!isPublic && !isProtected && !isPrivate) {
			return PACKAGE;
		}

		// assume public by default
		return PUBLIC;
	}
}
