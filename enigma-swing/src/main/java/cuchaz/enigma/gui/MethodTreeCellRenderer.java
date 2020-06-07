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

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.gui.config.UiConfig;

class MethodTreeCellRenderer implements TreeCellRenderer {

	private final TreeCellRenderer parent;

	MethodTreeCellRenderer(TreeCellRenderer parent) {
		this.parent = parent;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component ret = parent.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		if (!(value instanceof MethodInheritanceTreeNode) || ((MethodInheritanceTreeNode) value).isImplemented()) {
			ret.setForeground(UiConfig.getTextColor());
			ret.setFont(ret.getFont().deriveFont(Font.PLAIN));
		} else {
			ret.setForeground(UiConfig.getNumberColor());
			ret.setFont(ret.getFont().deriveFont(Font.ITALIC));
		}
		return ret;
	}
}
