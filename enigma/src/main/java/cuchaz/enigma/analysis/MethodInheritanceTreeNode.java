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

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class MethodInheritanceTreeNode extends DefaultMutableTreeNode {
	private final Translator translator;
	private MethodEntry entry;
	private boolean implemented;

	public MethodInheritanceTreeNode(Translator translator, MethodEntry entry, boolean implemented) {
		this.translator = translator;
		this.entry = entry;
		this.implemented = implemented;
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

	/**
	 * Returns the method entry represented by this tree node.
	 */
	public MethodEntry getMethodEntry() {
		return this.entry;
	}

	public boolean isImplemented() {
		return this.implemented;
	}

	@Override
	public String toString() {
		MethodEntry translatedEntry = translator.translate(entry);

		if (!this.implemented) {
			return translatedEntry.getParent().getFullName();
		} else {
			return translatedEntry.getFullName() + "()";
		}
	}

	/**
	 * Returns true if there is sub-node worthy to display.
	 */
	public boolean load(JarIndex index) {
		// get all the child nodes
		EntryIndex entryIndex = index.getEntryIndex();
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		boolean ret = false;

		for (ClassEntry inheritorEntry : inheritanceIndex.getChildren(this.entry.getParent())) {
			MethodEntry methodEntry = new MethodEntry(inheritorEntry, this.entry.getName(), this.entry.getDesc());

			MethodInheritanceTreeNode node = new MethodInheritanceTreeNode(translator, methodEntry, entryIndex.hasMethod(methodEntry));
			boolean childOverride = node.load(index);

			if (childOverride || node.implemented) {
				this.add(node);
				ret = true;
			}
		}

		return ret;
	}
}
