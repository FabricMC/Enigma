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

package cuchaz.enigma.mapping.entry;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.FieldMapping;
import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.MethodMapping;

public class EntryFactory {
	public static ClassEntry getObfClassEntry(JarIndex jarIndex, ClassMapping classMapping) {
		ClassEntry obfClassEntry = new ClassEntry(classMapping.getObfFullName());
		return obfClassEntry.buildClassEntry(jarIndex.getObfClassChain(obfClassEntry));
	}

	private static ClassEntry getObfClassEntry(ClassMapping classMapping) {
		return new ClassEntry(classMapping.getObfFullName());
	}

	public static ClassEntry getDeobfClassEntry(ClassMapping classMapping) {
		return new ClassEntry(classMapping.getDeobfName());
	}

	public static FieldEntry getObfFieldEntry(ClassMapping classMapping, FieldMapping fieldMapping) {
		return new FieldEntry(getObfClassEntry(classMapping), fieldMapping.getObfName(), fieldMapping.getObfDesc());
	}

	public static MethodEntry getMethodEntry(ClassEntry classEntry, String name, MethodDescriptor desc) {
		return new MethodEntry(classEntry, name, desc);
	}

	public static MethodEntry getObfMethodEntry(ClassEntry classEntry, MethodMapping methodMapping) {
		return getMethodEntry(classEntry, methodMapping.getObfName(), methodMapping.getObfDesc());
	}

	public static MethodEntry getObfMethodEntry(ClassMapping classMapping, MethodMapping methodMapping) {
		return getObfMethodEntry(getObfClassEntry(classMapping), methodMapping);
	}
}
