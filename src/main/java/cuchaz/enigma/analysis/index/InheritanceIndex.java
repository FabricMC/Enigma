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
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

public class InheritanceIndex implements JarIndexer {
	private Multimap<ClassEntry, ClassEntry> classParents = HashMultimap.create();
	private Multimap<ClassEntry, ClassEntry> classChildren = HashMultimap.create();

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

	public boolean hasParents(ClassEntry classEntry) {
		Collection<ClassEntry> parents = classParents.get(classEntry);
		return parents != null && !parents.isEmpty();
	}
}
