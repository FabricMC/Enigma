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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class MethodImplementationsTreeNode extends MethodTreeNode {
	public MethodImplementationsTreeNode(Translator translator, MethodEntry entry) {
		super(translator, entry);

		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
	}

	public static MethodImplementationsTreeNode findNode(MethodImplementationsTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.getMethodEntry().equals(entry)) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			MethodImplementationsTreeNode foundNode = findNode((MethodImplementationsTreeNode) node.getChildAt(i), entry);

			if (foundNode != null) {
				return foundNode;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		MethodEntry translatedEntry = translator.translate(this.getMethodEntry());
		assert translatedEntry != null;
		return translatedEntry.getFullName() + "()";
	}

	public void load(JarIndex index) {
		// get all method implementations
		List<MethodImplementationsTreeNode> nodes = new ArrayList<>();
		EntryIndex entryIndex = index.getEntryIndex();
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		Collection<ClassEntry> descendants = inheritanceIndex.getDescendants(this.getMethodEntry().getParent());

		for (ClassEntry inheritor : descendants) {
			MethodEntry methodEntry = this.getMethodEntry().withParent(inheritor);

			if (entryIndex.hasMethod(methodEntry)) {
				nodes.add(new MethodImplementationsTreeNode(translator, methodEntry));
			}
		}

		// add them to this node
		nodes.forEach(this::add);
	}
}
