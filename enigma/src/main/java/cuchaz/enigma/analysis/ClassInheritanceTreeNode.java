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

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import com.google.common.collect.Lists;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class ClassInheritanceTreeNode extends ClassTreeNode {
	public ClassInheritanceTreeNode(Translator translator, String obfClassName) {
		super(translator, new ClassEntry(obfClassName));
	}

	public static ClassInheritanceTreeNode findNode(ClassInheritanceTreeNode node, ClassEntry entry) {
		// is this the node?
		if (node.getObfClassName().equals(entry.getFullName())) {
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
		return this.getClassEntry().getFullName();
	}

	@Override
	public String toString() {
		return this.translator.translate(this.getClassEntry()).getFullName();
	}

	public void load(InheritanceIndex ancestries, boolean recurse) {
		// get all the child nodes
		List<ClassInheritanceTreeNode> nodes = Lists.newArrayList();

		for (ClassEntry inheritor : ancestries.getChildren(this.getClassEntry())) {
			nodes.add(new ClassInheritanceTreeNode(translator, inheritor.getFullName()));
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
