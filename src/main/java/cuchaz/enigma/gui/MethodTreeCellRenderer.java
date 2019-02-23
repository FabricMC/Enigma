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

import cuchaz.enigma.analysis.MethodInheritanceTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class MethodTreeCellRenderer implements TreeCellRenderer {

	private final DefaultTreeCellRenderer parent;

	public MethodTreeCellRenderer(DefaultTreeCellRenderer parent) {
		this.parent = parent;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		DefaultTreeCellRenderer ret = (DefaultTreeCellRenderer) parent.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		if (value instanceof MethodInheritanceTreeNode && ((MethodInheritanceTreeNode) value).isImplemented()) {
			Font old = ret.getFont();
			ret.setFont(old.deriveFont(Font.ITALIC));
			ret.setForeground(Color.GREEN);
		} else {
			ret.setFont(ret.getFont().deriveFont(Font.PLAIN));
		}
		return ret;
	}
}
