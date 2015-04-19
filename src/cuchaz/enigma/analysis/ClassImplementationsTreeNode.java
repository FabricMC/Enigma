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

public class ClassImplementationsTreeNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 3112703459157851912L;
	
	private Translator m_deobfuscatingTranslator;
	private ClassEntry m_entry;
	
	public ClassImplementationsTreeNode(Translator deobfuscatingTranslator, ClassEntry entry) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
	}
	
	public ClassEntry getClassEntry() {
		return m_entry;
	}
	
	public String getDeobfClassName() {
		return m_deobfuscatingTranslator.translateClass(m_entry.getClassName());
	}
	
	@Override
	public String toString() {
		String className = getDeobfClassName();
		if (className == null) {
			className = m_entry.getClassName();
		}
		return className;
	}
	
	public void load(JarIndex index) {
		// get all method implementations
		List<ClassImplementationsTreeNode> nodes = Lists.newArrayList();
		for (String implementingClassName : index.getImplementingClasses(m_entry.getClassName())) {
			nodes.add(new ClassImplementationsTreeNode(m_deobfuscatingTranslator, new ClassEntry(implementingClassName)));
		}
		
		// add them to this node
		for (ClassImplementationsTreeNode node : nodes) {
			this.add(node);
		}
	}
	
	public static ClassImplementationsTreeNode findNode(ClassImplementationsTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.m_entry.equals(entry)) {
			return node;
		}
		
		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			ClassImplementationsTreeNode foundNode = findNode((ClassImplementationsTreeNode)node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}
}
