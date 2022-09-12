package cuchaz.enigma.gui.renderer;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.util.GuiUtil;

public class ImplementationsTreeCellRenderer extends DefaultTreeCellRenderer {
	private final Gui gui;

	public ImplementationsTreeCellRenderer(Gui gui) {
		this.gui = gui;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		this.setForeground(UiConfig.getTextColor());

		if (value instanceof ClassImplementationsTreeNode node) {
			this.setIcon(GuiUtil.getClassIcon(this.gui, node.getClassEntry()));
		} else if (value instanceof MethodImplementationsTreeNode node) {
			this.setIcon(GuiUtil.getMethodIcon(node.getMethodEntry()));
		}

		return c;
	}
}
