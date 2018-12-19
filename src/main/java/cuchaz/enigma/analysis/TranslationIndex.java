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

package cuchaz.enigma.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.ReferencedEntryPool;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.entry.*;

import java.util.*;

public class TranslationIndex {

	private final ReferencedEntryPool entryPool;
	private Map<ClassEntry, ClassEntry> superclasses;
	private Map<Entry, DefEntry> defEntries = new HashMap<>();
	private Multimap<ClassEntry, FieldDefEntry> fieldEntries;
	private Multimap<ClassEntry, MethodDefEntry> methodEntries;
	private Multimap<ClassEntry, ClassEntry> interfaces;

	public TranslationIndex(ReferencedEntryPool entryPool) {
		this.entryPool = entryPool;
		this.superclasses = Maps.newHashMap();
		this.fieldEntries = HashMultimap.create();
		this.methodEntries = HashMultimap.create();
		this.interfaces = HashMultimap.create();

		for (FieldDefEntry entry : fieldEntries.values()) {
			defEntries.put(entry, entry);
		}

		for (MethodDefEntry entry : methodEntries.values()) {
			defEntries.put(entry, entry);
		}
	}

	public TranslationIndex(TranslationIndex other, Translator translator) {
		this.entryPool = other.entryPool;

		// translate the superclasses
		this.superclasses = Maps.newHashMap();
		for (Map.Entry<ClassEntry, ClassEntry> mapEntry : other.superclasses.entrySet()) {
			this.superclasses.put(translator.translate(mapEntry.getKey()), translator.translate(mapEntry.getValue()));
		}

		// translate the interfaces
		this.interfaces = HashMultimap.create();
		for (Map.Entry<ClassEntry, ClassEntry> mapEntry : other.interfaces.entries()) {
			this.interfaces.put(
					translator.translate(mapEntry.getKey()),
					translator.translate(mapEntry.getValue())
			);
		}

		// translate the fields
		this.fieldEntries = HashMultimap.create();
		for (Map.Entry<ClassEntry, FieldDefEntry> mapEntry : other.fieldEntries.entries()) {
			this.fieldEntries.put(
					translator.translate(mapEntry.getKey()),
					translator.translate(mapEntry.getValue())
			);
		}

		this.methodEntries = HashMultimap.create();
		for (Map.Entry<ClassEntry, MethodDefEntry> mapEntry : other.methodEntries.entries()) {
			this.methodEntries.put(
					translator.translate(mapEntry.getKey()),
					translator.translate(mapEntry.getValue())
			);
		}

		for (FieldDefEntry entry : fieldEntries.values()) {
			defEntries.put(entry, entry);
		}

		for (MethodDefEntry entry : methodEntries.values()) {
			defEntries.put(entry, entry);
		}
	}

	protected ClassDefEntry indexClass(int access, String name, String signature, String superName, String[] interfaces) {
		ClassDefEntry classEntry = new ClassDefEntry(name, Signature.createSignature(signature), new AccessFlags(access));
		if (isJre(classEntry)) {
			return null;
		}

		// add the superclass
		ClassEntry superclassEntry = entryPool.getClass(superName);
		if (superclassEntry != null) {
			this.superclasses.put(classEntry, superclassEntry);
		}

		// add the interfaces
		for (String interfaceClassName : interfaces) {
			ClassEntry interfaceClassEntry = entryPool.getClass(interfaceClassName);
			if (!isJre(interfaceClassEntry)) {
				this.interfaces.put(classEntry, interfaceClassEntry);
			}
		}

		return classEntry;
	}

	protected void indexField(FieldDefEntry fieldEntry) {
		this.fieldEntries.put(fieldEntry.getParent(), fieldEntry);
		this.defEntries.put(fieldEntry, fieldEntry);
	}

	protected void indexMethod(MethodDefEntry methodEntry) {
		this.methodEntries.put(methodEntry.getParent(), methodEntry);
		this.defEntries.put(methodEntry, methodEntry);
	}

	public ClassEntry getSuperclass(ClassEntry classEntry) {
		return this.superclasses.get(classEntry);
	}

	public List<ClassEntry> getAncestry(ClassEntry classEntry) {
		List<ClassEntry> ancestors = Lists.newArrayList();
		while (classEntry != null) {
			classEntry = getSuperclass(classEntry);
			if (classEntry != null) {
				ancestors.add(classEntry);
			}
		}
		return ancestors;
	}

	public List<ClassEntry> getImplementers(ClassEntry classEntry) {
		// linear search is fast enough for now
		List<ClassEntry> implementers = Lists.newArrayList();
		for (ClassEntry itf : this.interfaces.keySet()) {
			if (this.interfaces.containsEntry(itf, classEntry)) {
				implementers.add(itf);
			}
		}
		return implementers;
	}

	public List<ClassEntry> getSubclass(ClassEntry classEntry) {
		// linear search is fast enough for now
		List<ClassEntry> subclasses = Lists.newArrayList();
		for (Map.Entry<ClassEntry, ClassEntry> entry : this.superclasses.entrySet()) {
			ClassEntry subclass = entry.getKey();
			ClassEntry superclass = entry.getValue();
			if (classEntry.equals(superclass)) {
				subclasses.add(subclass);
			}
		}
		return subclasses;
	}

	public void getSubclassesRecursively(Set<ClassEntry> out, ClassEntry classEntry) {
		for (ClassEntry subclassEntry : getSubclass(classEntry)) {
			out.add(subclassEntry);
			getSubclassesRecursively(out, subclassEntry);
		}
	}

	public void getSubclassNamesRecursively(Set<String> out, ClassEntry classEntry) {
		for (ClassEntry subclassEntry : getSubclass(classEntry)) {
			out.add(subclassEntry.getName());
			getSubclassNamesRecursively(out, subclassEntry);
		}
	}

	public Collection<Map.Entry<ClassEntry, ClassEntry>> getClassInterfaces() {
		return this.interfaces.entries();
	}

	public Collection<ClassEntry> getInterfaces(ClassEntry classEntry) {
		return this.interfaces.get(classEntry);
	}

	public boolean isInterface(ClassEntry classEntry) {
		return this.interfaces.containsValue(classEntry);
	}

	public boolean entryExists(Entry entry) {
		if (entry == null) {
			return false;
		}

		if (entry instanceof FieldEntry) {
			return fieldExists((FieldEntry) entry);
		} else if (entry instanceof MethodEntry) {
			return methodExists((MethodEntry) entry);
		} else if (entry instanceof LocalVariableEntry) {
			return methodExists(((LocalVariableEntry) entry).getParent());
		}
		throw new IllegalArgumentException("Cannot check existence for " + entry.getClass());
	}

	public boolean fieldExists(FieldEntry fieldEntry) {
		return this.fieldEntries.containsEntry(fieldEntry.getParent(), fieldEntry);
	}

	public boolean methodExists(MethodEntry methodEntry) {
		return this.methodEntries.containsEntry(methodEntry.getParent(), methodEntry);
	}

	@SuppressWarnings("unchecked")
	public ClassEntry resolveEntryOwner(Entry entry) {
		if (entry instanceof ClassEntry) {
			return (ClassEntry) entry;
		}

		if (entryExists(entry)) {
			return entry.getContainingClass();
		}

		if (entry instanceof ChildEntry) {
			DefEntry def = defEntries.get(entry);
			if (def != null && (def.getAccess().isPrivate())) {
				return null;
			}

			// if we're protected/public/non-static, chances are we're somewhere down

			// find the child of a class in our ancestry
			List<Entry> ancestry = entry.getAncestry();
			for (int i = ancestry.size() - 1; i > 0; i--) {
				Entry child = ancestry.get(i);
				if (child instanceof ChildEntry && ((ChildEntry) child).getParent() instanceof ClassEntry) {
					return resolveEntryOwner((ChildEntry<ClassEntry>) child);
				}
			}
		}

		return null;
	}

	private ClassEntry resolveEntryOwner(ChildEntry<ClassEntry> childEntry) {
		DefEntry def = defEntries.get(childEntry);
		if (def != null && (def.getAccess().isPrivate())) {
			return null;
		}

		LinkedList<ClassEntry> classEntries = new LinkedList<>();
		classEntries.add(childEntry.getContainingClass());

		while (!classEntries.isEmpty()) {
			ClassEntry parent = classEntries.remove();
			ChildEntry<ClassEntry> cEntry = childEntry.withParent(parent);

			if (entryExists(cEntry)) {
				def = defEntries.get(cEntry);
				if (def == null || (!def.getAccess().isPrivate())) {
					return cEntry.getParent();
				}
			}

			ClassEntry superclass = getSuperclass(parent);
			if (superclass != null) {
				classEntries.add(superclass);
			}
			if (childEntry instanceof MethodEntry) {
				classEntries.addAll(getInterfaces(parent));
			}
		}

		return null;
	}

	private boolean isJre(ClassEntry classEntry) {
		String packageName = classEntry.getPackageName();
		return packageName != null && (packageName.startsWith("java") || packageName.startsWith("javax"));
	}
}
