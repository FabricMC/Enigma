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
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

// TODO: These have probably been made obf -- need to fix that
public class MethodImplementationsTreeNode extends DefaultMutableTreeNode {

	private final Translator translator;
	private MethodEntry entry;

	public MethodImplementationsTreeNode(Translator translator, MethodEntry entry) {
		this.translator = translator;
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}

		this.entry = entry;
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

	public MethodEntry getMethodEntry() {
		return this.entry;
	}

	@Override
	public String toString() {
		MethodEntry translatedEntry = translator.translate(entry);
		String className = translatedEntry.getParent().getFullName();
		String methodName = translatedEntry.getName();
		return className + "." + methodName + "()";
	}

	public void load(JarIndex index) {
		// get all method implementations
		List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
		for (String implementingClassName : index.getImplementingClasses(this.entry.getParent().getFullName())) {
			MethodEntry methodEntry = new MethodEntry(new ClassEntry(implementingClassName), this.entry.getName(), this.entry.getDesc());
			if (index.containsObfMethod(methodEntry)) {
				nodes.add(new MethodImplementationsTreeNode(translator, methodEntry));
			}
		}

		// add them to this node
		nodes.forEach(this::add);
	}
}
