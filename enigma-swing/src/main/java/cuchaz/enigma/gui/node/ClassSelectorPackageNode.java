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

package cuchaz.enigma.gui.node;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public class ClassSelectorPackageNode extends DefaultMutableTreeNode {

	private String packageName;

	public ClassSelectorPackageNode(String packageName) {
		this.packageName = packageName != null ? packageName : "(none)";
	}

	public String getPackageName() {
		return packageName;
	}

	@Override
	public Object getUserObject() {
		return packageName;
	}

	@Override
	public void setUserObject(Object userObject) {
		if (userObject instanceof String)
			this.packageName = (String) userObject;
		super.setUserObject(userObject);
	}

	@Override
	public String toString() {
		return !packageName.equals("(none)") ? ClassEntry.getNameInPackage(this.packageName) : "(none)";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassSelectorPackageNode && equals((ClassSelectorPackageNode) other);
	}

	@Override
	public int hashCode() {
		return packageName.hashCode();
	}

	public boolean equals(ClassSelectorPackageNode other) {
		return other != null && this.packageName.equals(other.packageName);
	}
}
