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

import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.Translator;

public class BehaviorReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<BehaviorEntry,BehaviorEntry> {
	
	private static final long serialVersionUID = -3658163700783307520L;
	
	private Translator m_deobfuscatingTranslator;
	private BehaviorEntry m_entry;
	private EntryReference<BehaviorEntry,BehaviorEntry> m_reference;
	private Access m_access;
	
	public BehaviorReferenceTreeNode(Translator deobfuscatingTranslator, BehaviorEntry entry) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
		m_reference = null;
	}
	
	public BehaviorReferenceTreeNode(Translator deobfuscatingTranslator, EntryReference<BehaviorEntry,BehaviorEntry> reference, Access access) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = reference.entry;
		m_reference = reference;
		m_access = access;
	}
	
	@Override
	public BehaviorEntry getEntry() {
		return m_entry;
	}
	
	@Override
	public EntryReference<BehaviorEntry,BehaviorEntry> getReference() {
		return m_reference;
	}
	
	@Override
	public String toString() {
		if (m_reference != null) {
			return String.format("%s (%s)", m_deobfuscatingTranslator.translateEntry(m_reference.context), m_access);
		}
		return m_deobfuscatingTranslator.translateEntry(m_entry).toString();
	}
	
	public void load(JarIndex index, boolean recurse) {
		// get all the child nodes
		for (EntryReference<BehaviorEntry,BehaviorEntry> reference : index.getBehaviorReferences(m_entry)) {
			add(new BehaviorReferenceTreeNode(m_deobfuscatingTranslator, reference, index.getAccess(m_entry)));
		}
		
		if (recurse && children != null) {
			for (Object child : children) {
				if (child instanceof BehaviorReferenceTreeNode) {
					BehaviorReferenceTreeNode node = (BehaviorReferenceTreeNode)child;
					
					// don't recurse into ancestor
					Set<Entry> ancestors = Sets.newHashSet();
					TreeNode n = (TreeNode)node;
					while (n.getParent() != null) {
						n = n.getParent();
						if (n instanceof BehaviorReferenceTreeNode) {
							ancestors.add( ((BehaviorReferenceTreeNode)n).getEntry());
						}
					}
					if (ancestors.contains(node.getEntry())) {
						continue;
					}
					
					node.load(index, true);
				}
			}
		}
	}
}
