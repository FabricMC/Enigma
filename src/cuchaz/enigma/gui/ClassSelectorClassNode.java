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
package cuchaz.enigma.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.mapping.ClassEntry;

public class ClassSelectorClassNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = -8956754339813257380L;
	
	private ClassEntry m_classEntry;
	
	public ClassSelectorClassNode(ClassEntry classEntry) {
		m_classEntry = classEntry;
	}
	
	public ClassEntry getClassEntry() {
		return m_classEntry;
	}
	
	@Override
	public String toString() {
		if (m_classEntry instanceof ScoredClassEntry) {
			return String.format("%d%% %s", (int)((ScoredClassEntry)m_classEntry).getScore(), m_classEntry.getSimpleName());
		}
		return m_classEntry.getSimpleName();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassSelectorClassNode) {
			return equals((ClassSelectorClassNode)other);
		}
		return false;
	}
	
	public boolean equals(ClassSelectorClassNode other) {
		return m_classEntry.equals(other.m_classEntry);
	}
}
