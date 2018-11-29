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

import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.TypeDescriptor;

import java.util.HashMap;
import java.util.Map;

public class ReferencedEntryPool {
	private final Map<String, ClassEntry> classEntries = new HashMap<>();
	private final Map<String, Map<String, MethodEntry>> methodEntries = new HashMap<>();
	private final Map<String, Map<String, FieldEntry>> fieldEntries = new HashMap<>();

	public ClassEntry getClass(String name) {
		// TODO: FIXME - I'm a hack!
		if ("[T".equals(name) || "[[T".equals(name) || "[[[T".equals(name)) {
			name = name.replaceAll("T", "Ljava/lang/Object;");
		}

		final String computeName = name;
		return this.classEntries.computeIfAbsent(name, s -> new ClassEntry(computeName));
	}

	public MethodEntry getMethod(ClassEntry ownerEntry, String name, String desc) {
		return getMethod(ownerEntry, name, new MethodDescriptor(desc));
	}

	public MethodEntry getMethod(ClassEntry ownerEntry, String name, MethodDescriptor desc) {
		String key = name + desc.toString();
		return getClassMethods(ownerEntry.getName()).computeIfAbsent(key, s -> new MethodEntry(ownerEntry, name, desc));
	}

	public FieldEntry getField(ClassEntry ownerEntry, String name, String desc) {
		return getField(ownerEntry, name, new TypeDescriptor(desc));
	}

	public FieldEntry getField(ClassEntry ownerEntry, String name, TypeDescriptor desc) {
		return getClassFields(ownerEntry.getName()).computeIfAbsent(name, s -> new FieldEntry(ownerEntry, name, desc));
	}

	private Map<String, MethodEntry> getClassMethods(String name) {
		return methodEntries.computeIfAbsent(name, s -> new HashMap<>());
	}

	private Map<String, FieldEntry> getClassFields(String name) {
		return fieldEntries.computeIfAbsent(name, s -> new HashMap<>());
	}
}
