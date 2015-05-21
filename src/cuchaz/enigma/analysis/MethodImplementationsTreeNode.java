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

public class MethodImplementationsTreeNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 3781080657461899915L;
	
	private Translator m_deobfuscatingTranslator;
	private MethodEntry m_entry;
	
	public MethodImplementationsTreeNode(Translator deobfuscatingTranslator, MethodEntry entry) {
		if (entry == null) {
			throw new IllegalArgumentException("entry cannot be null!");
		}
		
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
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
	
	@Override
	public String toString() {
		String className = getDeobfClassName();
		if (className == null) {
			className = m_entry.getClassName();
		}
		
		String methodName = getDeobfMethodName();
		if (methodName == null) {
			methodName = m_entry.getName();
		}
		return className + "." + methodName + "()";
	}
	
	public void load(JarIndex index) {
		
		// get all method implementations
		List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
		for (String implementingClassName : index.getImplementingClasses(m_entry.getClassName())) {
			MethodEntry methodEntry = new MethodEntry(
				new ClassEntry(implementingClassName),
				m_entry.getName(),
				m_entry.getSignature()
			);
			if (index.containsObfBehavior(methodEntry)) {
				nodes.add(new MethodImplementationsTreeNode(m_deobfuscatingTranslator, methodEntry));
			}
		}
		
		// add them to this node
		for (MethodImplementationsTreeNode node : nodes) {
			this.add(node);
		}
	}
	
	public static MethodImplementationsTreeNode findNode(MethodImplementationsTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.getMethodEntry().equals(entry)) {
			return node;
		}
		
		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			MethodImplementationsTreeNode foundNode = findNode((MethodImplementationsTreeNode)node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}
}
