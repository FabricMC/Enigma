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

public class ClassSelectorPackageNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = -3730868701219548043L;
	
	private String m_packageName;
	
	public ClassSelectorPackageNode(String packageName) {
		m_packageName = packageName;
	}
	
	public String getPackageName() {
		return m_packageName;
	}
	
	@Override
	public String toString() {
		return m_packageName;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassSelectorPackageNode) {
			return equals((ClassSelectorPackageNode)other);
		}
		return false;
	}
	
	public boolean equals(ClassSelectorPackageNode other) {
		return m_packageName.equals(other.m_packageName);
	}
}
