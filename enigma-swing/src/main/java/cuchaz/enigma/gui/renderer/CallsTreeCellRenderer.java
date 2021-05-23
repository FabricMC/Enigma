package cuchaz.enigma.gui.renderer;

import cuchaz.enigma.analysis.*;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class CallsTreeCellRenderer extends DefaultTreeCellRenderer {
    private final Gui gui;

    public CallsTreeCellRenderer(Gui gui) {
        this.gui = gui;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        EntryReference<?, ?> reference = ((ReferenceTreeNode<?, ?>) value).getReference();

        this.setForeground(UiConfig.getTextColor());

        // if the node represents the method calling the entry
        if (reference != null) {
            if (reference.context instanceof MethodEntry) {
                this.setIcon(GuiUtil.getMethodIcon((MethodEntry) reference.context));
            }
        // if the node represents the called entry
        } else {
            if (value instanceof ClassReferenceTreeNode node) {
                this.setIcon(GuiUtil.getClassIcon(this.gui, node.getEntry()));
            } else if (value instanceof MethodReferenceTreeNode node) {
                this.setIcon(GuiUtil.getMethodIcon(node.getEntry()));
            } else if (value instanceof FieldReferenceTreeNode) {
                this.setIcon(GuiUtil.FIELD_ICON);
            }
        }

        return c;
    }
}
