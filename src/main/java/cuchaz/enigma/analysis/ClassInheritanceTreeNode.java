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

import com.google.common.collect.Lists;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class ClassInheritanceTreeNode extends DefaultMutableTreeNode {

	private final ClassEntry obfClassEntry;

	public ClassInheritanceTreeNode(String obfClassName) {
		this.obfClassEntry = new ClassEntry(obfClassName);
	}

	public static ClassInheritanceTreeNode findNode(ClassInheritanceTreeNode node, ClassEntry entry) {
		// is this the node?
		if (node.getObfClassName().equals(entry.getName())) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			ClassInheritanceTreeNode foundNode = findNode((ClassInheritanceTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}

	public String getObfClassName() {
		return this.obfClassEntry.getName();
	}

	@Override
	public String toString() {
		return this.obfClassEntry.getName();
	}

	public void load(TranslationIndex ancestries, boolean recurse) {
		// get all the child nodes
		List<ClassInheritanceTreeNode> nodes = Lists.newArrayList();
		for (ClassEntry subclassEntry : ancestries.getSubclass(this.obfClassEntry)) {
			nodes.add(new ClassInheritanceTreeNode(subclassEntry.getName()));
		}

		// add them to this node
		nodes.forEach(this::add);

		if (recurse) {
			for (ClassInheritanceTreeNode node : nodes) {
				node.load(ancestries, true);
			}
		}
	}
}
