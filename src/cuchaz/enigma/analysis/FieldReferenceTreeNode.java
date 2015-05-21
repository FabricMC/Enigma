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

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Translator;

public class FieldReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<FieldEntry,BehaviorEntry> {
	
	private static final long serialVersionUID = -7934108091928699835L;
	
	private Translator m_deobfuscatingTranslator;
	private FieldEntry m_entry;
	private EntryReference<FieldEntry,BehaviorEntry> m_reference;
	private Access m_access;
	
	public FieldReferenceTreeNode(Translator deobfuscatingTranslator, FieldEntry entry) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
		m_reference = null;
	}
	
	private FieldReferenceTreeNode(Translator deobfuscatingTranslator, EntryReference<FieldEntry,BehaviorEntry> reference, Access access) {
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = reference.entry;
		m_reference = reference;
		m_access = access;
	}
	
	@Override
	public FieldEntry getEntry() {
		return m_entry;
	}
	
	@Override
	public EntryReference<FieldEntry,BehaviorEntry> getReference() {
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
		if (m_reference == null) {
			for (EntryReference<FieldEntry,BehaviorEntry> reference : index.getFieldReferences(m_entry)) {
				add(new FieldReferenceTreeNode(m_deobfuscatingTranslator, reference, index.getAccess(m_entry)));
			}
		} else {
			for (EntryReference<BehaviorEntry,BehaviorEntry> reference : index.getBehaviorReferences(m_reference.context)) {
				add(new BehaviorReferenceTreeNode(m_deobfuscatingTranslator, reference, index.getAccess(m_reference.context)));
			}
		}
		
		if (recurse && children != null) {
			for (Object node : children) {
				if (node instanceof BehaviorReferenceTreeNode) {
					((BehaviorReferenceTreeNode)node).load(index, true);
				} else if (node instanceof FieldReferenceTreeNode) {
					((FieldReferenceTreeNode)node).load(index, true);
				}
			}
		}
	}
}
