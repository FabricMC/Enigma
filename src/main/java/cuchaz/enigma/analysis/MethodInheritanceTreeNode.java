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
import cuchaz.enigma.mapping.entry.ClassEntry;
import cuchaz.enigma.mapping.entry.MethodEntry;
import cuchaz.enigma.mapping.Translator;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class MethodInheritanceTreeNode extends DefaultMutableTreeNode {

	private Translator deobfuscatingTranslator;
	private MethodEntry entry;
	private boolean isImplemented;

	public MethodInheritanceTreeNode(Translator deobfuscatingTranslator, MethodEntry entry, boolean isImplemented) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
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

	public String getDeobfClassName() {
		return this.deobfuscatingTranslator.getTranslatedClass(this.entry.getOwnerClassEntry()).getName();
	}

	public String getDeobfMethodName() {
		return this.deobfuscatingTranslator.getTranslatedMethod(this.entry).getName();
	}

	public boolean isImplemented() {
		return this.isImplemented;
	}

	@Override
	public String toString() {
		String className = getDeobfClassName();
		if (className == null) {
			className = this.entry.getClassName();
		}

		if (!this.isImplemented) {
			return className;
		} else {
			String methodName = getDeobfMethodName();
			if (methodName == null) {
				methodName = this.entry.getName();
			}
			return className + "." + methodName + "()";
		}
	}

	public void load(JarIndex index, boolean recurse) {
		// get all the child nodes
		List<MethodInheritanceTreeNode> nodes = Lists.newArrayList();
		for (ClassEntry subclassEntry : index.getTranslationIndex().getSubclass(this.entry.getOwnerClassEntry())) {
			MethodEntry methodEntry = new MethodEntry(subclassEntry, this.entry.getName(), this.entry.getDesc());
			nodes.add(new MethodInheritanceTreeNode(this.deobfuscatingTranslator, methodEntry, index.containsObfMethod(methodEntry)));
		}

		// add them to this node
		nodes.forEach(this::add);

		if (recurse) {
			for (MethodInheritanceTreeNode node : nodes) {
				node.load(index, true);
			}
		}
	}
}
