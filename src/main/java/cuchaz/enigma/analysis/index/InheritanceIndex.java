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

package cuchaz.enigma.analysis.index;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.*;

import java.util.*;

public class InheritanceIndex implements JarIndexer, RemappableIndex {
	private final Multimap<ClassEntry, ClassEntry> classParents = HashMultimap.create();
	private final Multimap<ClassEntry, ClassEntry> classChildren = HashMultimap.create();

	@Override
	public InheritanceIndex remapped(Translator translator) {
		InheritanceIndex index = new InheritanceIndex();
		for (Map.Entry<ClassEntry, ClassEntry> entry : classChildren.entries()) {
			index.classChildren.put(translator.translate(entry.getKey()), translator.translate(entry.getValue()));
		}
		for (Map.Entry<ClassEntry, ClassEntry> entry : classParents.entries()) {
			index.classParents.put(translator.translate(entry.getKey()), translator.translate(entry.getValue()));
		}

		return index;
	}

	@Override
	public void remapEntry(Entry<?> entry, Entry<?> newEntry) {
		if (entry instanceof ClassEntry) {
			ClassEntry classEntry = (ClassEntry) entry;

			Collection<ClassEntry> parents = classParents.removeAll(classEntry);
			classParents.putAll((ClassEntry) newEntry, parents);

			// Find all the parents of this class and remap their children
			for (ClassEntry parent : parents) {
				classChildren.remove(parent, entry);
				classChildren.put(parent, (ClassEntry) newEntry);
			}

			Collection<ClassEntry> children = classChildren.removeAll(classEntry);
			classChildren.putAll((ClassEntry) newEntry, children);

			// Find all the children of this class and remap their parents
			for (ClassEntry child : children) {
				classParents.remove(child, entry);
				classParents.put(child, (ClassEntry) newEntry);
			}
		}
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		ClassEntry superClass = classEntry.getSuperClass();
		if (superClass != null) {
			indexParent(classEntry, superClass);
		}

		for (ClassEntry interfaceEntry : classEntry.getInterfaces()) {
			indexParent(classEntry, interfaceEntry);
		}
	}

	private void indexParent(ClassEntry childEntry, ClassEntry parentEntry) {
		if (childEntry.isJre() || parentEntry.isJre()) {
			return;
		}
		classParents.put(childEntry, parentEntry);
		classChildren.put(parentEntry, childEntry);
	}

	public Collection<ClassEntry> getParents(ClassEntry classEntry) {
		return classParents.get(classEntry);
	}

	public Collection<ClassEntry> getChildren(ClassEntry classEntry) {
		return classChildren.get(classEntry);
	}

	public Set<ClassEntry> getAncestors(ClassEntry classEntry) {
		Set<ClassEntry> ancestors = Sets.newHashSet();

		LinkedList<ClassEntry> ancestorQueue = new LinkedList<>();
		ancestorQueue.push(classEntry);

		while (!ancestorQueue.isEmpty()) {
			ClassEntry ancestor = ancestorQueue.pop();
			Collection<ClassEntry> parents = getParents(ancestor);

			parents.forEach(ancestorQueue::push);
			ancestors.addAll(parents);
		}

		return ancestors;
	}

	public boolean isParent(ClassEntry classEntry) {
		return classChildren.containsKey(classEntry);
	}
}
