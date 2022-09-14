/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.gui.renderer;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.util.GuiUtil;

public class InheritanceTreeCellRenderer extends DefaultTreeCellRenderer {
	private final Gui gui;

	public InheritanceTreeCellRenderer(Gui gui) {
		this.gui = gui;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component ret = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

		if (!(value instanceof MethodInheritanceTreeNode node) || node.isImplemented()) {
			ret.setForeground(UiConfig.getTextColor());
			ret.setFont(ret.getFont().deriveFont(Font.PLAIN));

			if (value instanceof ClassInheritanceTreeNode) {
				this.setIcon(GuiUtil.getClassIcon(this.gui, ((ClassInheritanceTreeNode) value).getClassEntry()));
			} else if (value instanceof MethodInheritanceTreeNode) {
				this.setIcon(GuiUtil.getMethodIcon(((MethodInheritanceTreeNode) value).getMethodEntry()));
			}
		} else {
			ret.setForeground(UiConfig.getNumberColor());
			ret.setFont(ret.getFont().deriveFont(Font.ITALIC));
			this.setIcon(GuiUtil.CLASS_ICON);
		}

		return ret;
	}
}
