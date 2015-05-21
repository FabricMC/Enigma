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
import cuchaz.enigma.mapping.Translator;

public class ClassInheritanceTreeNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 4432367405826178490L;
	
	private Translator m_deobfuscatingTranslator;
	private String m_obfClassName;
	
	public ClassInheritanceTreeNode(Translator deobfuscatingTranslator, String obfClassName) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_obfClassName = obfClassName;
	}
	
	public String getObfClassName() {
		return m_obfClassName;
	}
	
	public String getDeobfClassName() {
		return m_deobfuscatingTranslator.translateClass(m_obfClassName);
	}
	
	@Override
	public String toString() {
		String deobfClassName = getDeobfClassName();
		if (deobfClassName != null) {
			return deobfClassName;
		}
		return m_obfClassName;
	}
	
	public void load(TranslationIndex ancestries, boolean recurse) {
		// get all the child nodes
		List<ClassInheritanceTreeNode> nodes = Lists.newArrayList();
		for (ClassEntry subclassEntry : ancestries.getSubclass(new ClassEntry(m_obfClassName))) {
			nodes.add(new ClassInheritanceTreeNode(m_deobfuscatingTranslator, subclassEntry.getName()));
		}
		
		// add them to this node
		for (ClassInheritanceTreeNode node : nodes) {
			this.add(node);
		}
		
		if (recurse) {
			for (ClassInheritanceTreeNode node : nodes) {
				node.load(ancestries, true);
			}
		}
	}
	
	public static ClassInheritanceTreeNode findNode(ClassInheritanceTreeNode node, ClassEntry entry) {
		// is this the node?
		if (node.getObfClassName().equals(entry.getName())) {
			return node;
		}
		
		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			ClassInheritanceTreeNode foundNode = findNode((ClassInheritanceTreeNode)node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}
}
