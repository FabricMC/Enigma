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
package cuchaz.enigma.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.mapping.ClassEntry;

public class ClassSelectorClassNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = -8956754339813257380L;

    private ClassEntry classEntry;

    public ClassSelectorClassNode(ClassEntry classEntry) {
        this.classEntry = classEntry;
    }

    public ClassEntry getClassEntry() {
        return this.classEntry;
    }

    @Override
    public String toString() {
        if (this.classEntry instanceof ScoredClassEntry) {
            return String.format("%d%% %s", (int) ((ScoredClassEntry) this.classEntry).getScore(), this.classEntry.getSimpleName());
        }
        return this.classEntry.getSimpleName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClassSelectorClassNode && equals((ClassSelectorClassNode) other);
    }

    public boolean equals(ClassSelectorClassNode other) {
        return this.classEntry.equals(other.classEntry);
    }
}
