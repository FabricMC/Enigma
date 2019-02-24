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

import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public class MethodInheritanceTreeNode extends DefaultMutableTreeNode {

	private final Translator translator;
	private MethodEntry entry;
	private boolean isImplemented;

	public MethodInheritanceTreeNode(Translator translator, MethodEntry entry, boolean isImplemented) {
		this.translator = translator;
		this.entry = entry;
		this.isImplemented = isImplemented;
	}

	public static MethodInheritanceTreeNode findNode(MethodInheritanceTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.getMethodEntry().equals(entry)) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			MethodInheritanceTreeNode foundNode = findNode((MethodInheritanceTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}

	public MethodEntry getMethodEntry() {
		return this.entry;
	}

	public boolean isImplemented() {
		return this.isImplemented;
	}

	public boolean shouldExpand() {
		return this.isImplemented || !this.isLeaf();
	}

	@Override
	public String toString() {
		MethodEntry translatedEntry = translator.translate(entry);
		String className = translatedEntry.getContainingClass().getFullName();

		if (!this.isImplemented) {
			return className;
		} else {
			String methodName = translatedEntry.getName();
			return className + "." + methodName + "()";
		}
	}

	public void load(JarIndex index) {
		// get all the child nodes
		EntryIndex entryIndex = index.getEntryIndex();
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		for (ClassEntry inheritorEntry : inheritanceIndex.getChildren(this.entry.getParent())) {
			MethodEntry methodEntry = new MethodEntry(inheritorEntry, this.entry.getName(), this.entry.getDesc());

			MethodInheritanceTreeNode node = new MethodInheritanceTreeNode(translator, methodEntry, entryIndex.hasMethod(methodEntry));
			node.load(index);

			if (node.shouldExpand()) {
				this.add(node);
			}
		}
	}
}
