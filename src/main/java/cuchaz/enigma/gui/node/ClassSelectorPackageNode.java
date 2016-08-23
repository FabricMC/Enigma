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

import javax.swing.tree.DefaultMutableTreeNode;

public class ClassSelectorPackageNode extends DefaultMutableTreeNode {

    private String packageName;

    public ClassSelectorPackageNode(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override public void setUserObject(Object userObject)
    {
        if (userObject instanceof String)
            this.packageName = (String) userObject;
        super.setUserObject(userObject);
    }

    @Override
    public String toString() {
        return this.packageName;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClassSelectorPackageNode && equals((ClassSelectorPackageNode) other);
    }

    public boolean equals(ClassSelectorPackageNode other) {
        return this.packageName.equals(other.packageName);
    }
}
