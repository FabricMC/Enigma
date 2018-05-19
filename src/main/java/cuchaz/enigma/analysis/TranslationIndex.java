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
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.mapping.entry.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TranslationIndex {

	private final ReferencedEntryPool entryPool;
	private Map<ClassEntry, ClassEntry> superclasses;
	private Multimap<ClassEntry, FieldDefEntry> fieldEntries;
	private Multimap<ClassEntry, MethodDefEntry> methodEntries;
	private Multimap<ClassEntry, ClassEntry> interfaces;

	public TranslationIndex(ReferencedEntryPool entryPool) {
		this.entryPool = entryPool;
		this.superclasses = Maps.newHashMap();
		this.fieldEntries = HashMultimap.create();
		this.methodEntries = HashMultimap.create();
		this.interfaces = HashMultimap.create();
	}

	public TranslationIndex(TranslationIndex other, Translator translator) {
		this.entryPool = other.entryPool;

		// translate the superclasses
		this.superclasses = Maps.newHashMap();
		for (Map.Entry<ClassEntry, ClassEntry> mapEntry : other.superclasses.entrySet()) {
			this.superclasses.put(translator.getTranslatedClass(mapEntry.getKey()), translator.getTranslatedClass(mapEntry.getValue()));
		}

		// translate the interfaces
		this.interfaces = HashMultimap.create();
		for (Map.Entry<ClassEntry, ClassEntry> mapEntry : other.interfaces.entries()) {
			this.interfaces.put(
					translator.getTranslatedClass(mapEntry.getKey()),
					translator.getTranslatedClass(mapEntry.getValue())
			);
		}

		// translate the fields
		this.fieldEntries = HashMultimap.create();
		for (Map.Entry<ClassEntry, FieldDefEntry> mapEntry : other.fieldEntries.entries()) {
			this.fieldEntries.put(
					translator.getTranslatedClass(mapEntry.getKey()),
					translator.getTranslatedFieldDef(mapEntry.getValue())
			);
		}

		this.methodEntries = HashMultimap.create();
		for (Map.Entry<ClassEntry, MethodDefEntry> mapEntry : other.methodEntries.entries()) {
			this.methodEntries.put(
					translator.getTranslatedClass(mapEntry.getKey()),
					translator.getTranslatedMethodDef(mapEntry.getValue())
			);
		}
	}

	protected ClassDefEntry indexClass(int access, String name, String signature, String superName, String[] interfaces) {
		ClassDefEntry classEntry = new ClassDefEntry(name, Signature.createSignature(signature), new AccessFlags(access));
		if (isJre(classEntry)) {
			return null;
		}

		// add the superclass
		ClassEntry superclassEntry = entryPool.getClass(superName);
		if (!isJre(superclassEntry)) {
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
		this.fieldEntries.put(fieldEntry.getOwnerClassEntry(), fieldEntry);
	}

	protected void indexMethod(MethodDefEntry methodEntry) {
		this.methodEntries.put(methodEntry.getOwnerClassEntry(), methodEntry);
	}

	public void renameClasses(Map<String, String> renames) {
		EntryRenamer.renameClassesInMap(renames, this.superclasses);
		EntryRenamer.renameClassesInMultimap(renames, this.fieldEntries);
		EntryRenamer.renameClassesInMultimap(renames, this.methodEntries);
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
			return methodExists(((LocalVariableEntry) entry).getOwnerEntry());
		}
		throw new IllegalArgumentException("Cannot check existence for " + entry.getClass());
	}

	public boolean fieldExists(FieldEntry fieldEntry) {
		return this.fieldEntries.containsEntry(fieldEntry.getOwnerClassEntry(), fieldEntry);
	}

	public boolean methodExists(MethodEntry methodEntry) {
		return this.methodEntries.containsEntry(methodEntry.getOwnerClassEntry(), methodEntry);
	}

	public ClassEntry resolveEntryOwner(Entry entry) {
		return resolveEntryOwner(entry, false);
	}

	public ClassEntry resolveEntryOwner(Entry entry, boolean checkSuperclassBeforeChild) {
		if (entry instanceof ClassEntry) {
			return (ClassEntry) entry;
		}

		ClassEntry superclassEntry = resolveSuperclass(entry, checkSuperclassBeforeChild);
		if (superclassEntry != null) {
			return superclassEntry;
		}

		ClassEntry interfaceEntry = resolveInterface(entry);
		if (interfaceEntry != null) {
			return interfaceEntry;
		}

		return null;
	}

	public ClassEntry resolveSuperclass(Entry entry, boolean checkSuperclassBeforeChild) {

		// Default case
		if (!checkSuperclassBeforeChild)
			return resolveSuperclass(entry);

		// Save the original entry
		Entry originalEntry = entry;

		// Get all possible superclasses and reverse the list
		List<ClassEntry> superclasses = Lists.reverse(getAncestry(originalEntry.getOwnerClassEntry()));

		boolean existInEntry = false;

		for (ClassEntry classEntry : superclasses) {
			entry = entry.updateOwnership(classEntry);
			existInEntry = entryExists(entry);

			// Check for possible entry in interfaces of superclasses
			ClassEntry interfaceEntry = resolveInterface(entry);
			if (interfaceEntry != null)
				return interfaceEntry;
			if (existInEntry)
				break;
		}

		// Doesn't exists in superclasses? check the child or return null
		if (!existInEntry)
			return !entryExists(originalEntry) ? null : originalEntry.getOwnerClassEntry();

		return entry.getOwnerClassEntry();
	}

	public ClassEntry resolveSuperclass(Entry entry) {
		// this entry could refer to a method on a class where the method is not actually implemented
		// travel up the inheritance tree to find the closest implementation

		while (!entryExists(entry)) {
			// is there a parent class?
			ClassEntry superclassEntry = getSuperclass(entry.getOwnerClassEntry());
			if (superclassEntry == null) {
				// this is probably a method from a class in a library
				// we can't trace the implementation up any higher unless we index the library
				return null;
			}

			// move up to the parent class
			entry = entry.updateOwnership(superclassEntry);
		}
		return entry.getOwnerClassEntry();
	}

	public ClassEntry resolveInterface(Entry entry) {
		// the interfaces for any class is a forest
		// so let's look at all the trees

		for (ClassEntry interfaceEntry : this.interfaces.get(entry.getOwnerClassEntry())) {
			Collection<ClassEntry> subInterface = this.interfaces.get(interfaceEntry);
			if (subInterface != null && !subInterface.isEmpty()) {
				ClassEntry result = resolveInterface(entry.updateOwnership(interfaceEntry));
				if (result != null)
					return result;
			}
			ClassEntry resolvedClassEntry = resolveSuperclass(entry.updateOwnership(interfaceEntry));
			if (resolvedClassEntry != null) {
				return resolvedClassEntry;
			}
		}
		return null;
	}

	private boolean isJre(ClassEntry classEntry) {
		String packageName = classEntry.getPackageName();
		return packageName != null && (packageName.startsWith("java") || packageName.startsWith("javax"));
	}
}
