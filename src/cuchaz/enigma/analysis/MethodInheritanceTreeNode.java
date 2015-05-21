/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import com.google.common.collect.Lists;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class MethodInheritanceTreeNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 1096677030991810007L;
	
	private Translator m_deobfuscatingTranslator;
	private MethodEntry m_entry;
	private boolean m_isImplemented;
	
	public MethodInheritanceTreeNode(Translator deobfuscatingTranslator, MethodEntry entry, boolean isImplemented) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
		m_isImplemented = isImplemented;
	}
	
	public MethodEntry getMethodEntry() {
		return m_entry;
	}
	
	public String getDeobfClassName() {
		return m_deobfuscatingTranslator.translateClass(m_entry.getClassName());
	}
	
	public String getDeobfMethodName() {
		return m_deobfuscatingTranslator.translate(m_entry);
	}
	
	public boolean isImplemented() {
		return m_isImplemented;
	}
	
	@Override
	public String toString() {
		String className = getDeobfClassName();
		if (className == null) {
			className = m_entry.getClassName();
		}
		
		if (!m_isImplemented) {
			return className;
		} else {
			String methodName = getDeobfMethodName();
			if (methodName == null) {
				methodName = m_entry.getName();
			}
			return className + "." + methodName + "()";
		}
	}
	
	public void load(JarIndex index, boolean recurse) {
		// get all the child nodes
		List<MethodInheritanceTreeNode> nodes = Lists.newArrayList();
		for (ClassEntry subclassEntry : index.getTranslationIndex().getSubclass(m_entry.getClassEntry())) {
			MethodEntry methodEntry = new MethodEntry(
				subclassEntry,
				m_entry.getName(),
				m_entry.getSignature()
			);
			nodes.add(new MethodInheritanceTreeNode(
				m_deobfuscatingTranslator,
				methodEntry,
				index.containsObfBehavior(methodEntry)
			));
		}
		
		// add them to this node
		for (MethodInheritanceTreeNode node : nodes) {
			this.add(node);
		}
		
		if (recurse) {
			for (MethodInheritanceTreeNode node : nodes) {
				node.load(index, true);
			}
		}
	}
	
	public static MethodInheritanceTreeNode findNode(MethodInheritanceTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.getMethodEntry().equals(entry)) {
			return node;
		}
		
		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			MethodInheritanceTreeNode foundNode = findNode((MethodInheritanceTreeNode)node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}
}
