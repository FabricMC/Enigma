package cuchaz.enigma.gui.panels;

import cuchaz.enigma.analysis.StructureTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StructurePanel extends JPanel {
    private JPanel sortingPanel;
    private JCheckBox hideDeobfuscated;

    private JTree structureTree;

    public StructurePanel(Gui gui) {
        this.sortingPanel = new JPanel();
        this.hideDeobfuscated = new JCheckBox(I18n.translate("info_panel.tree.structure.hide_deobfuscated"));
        this.hideDeobfuscated.addActionListener(event -> gui.showStructure(gui.getActiveEditor()));
        this.sortingPanel.add(this.hideDeobfuscated);
        this.sortingPanel.setVisible(false);

        this.structureTree = new JTree();
        this.structureTree.setModel(null);
        this.structureTree.setCellRenderer(new StructureTreeCellRenderer(gui));
        this.structureTree.setSelectionModel(new SingleTreeSelectionModel());
        this.structureTree.setShowsRootHandles(true);
        this.structureTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
                    // get the selected node
                    TreePath path = structureTree.getSelectionPath();
                    if (path == null) {
                        return;
                    }

                    Object node = path.getLastPathComponent();
                    if (node instanceof StructureTreeNode) {
                        gui.getController().navigateTo(((StructureTreeNode) node).getEntry());
                    }
                }
            }
        });

        this.setLayout(new BorderLayout());
        this.add(this.sortingPanel, BorderLayout.NORTH);
        this.add(new JScrollPane(this.structureTree));
    }

    public JPanel getSortingPanel() {
        return this.sortingPanel;
    }

    /**
     * Returns whether the "Hide Deobfuscated" option of this structure panel is selected.
     */
    public boolean shouldHideDeobfuscated() {
        return this.hideDeobfuscated.isSelected();
    }

    public JTree getStructureTree() {
        return this.structureTree;
    }

    public void retranslateUi() {
        this.hideDeobfuscated.setText(I18n.translate("info_panel.tree.structure.hide_deobfuscated"));
    }

    class StructureTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Gui gui;

        StructureTreeCellRenderer(Gui gui) {
            this.gui = gui;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            ParentedEntry<?> entry = ((StructureTreeNode) value).getEntry();

            if (entry instanceof ClassEntry classEntry) {
                this.setIcon(GuiUtil.getClassIcon(gui, classEntry));
            } else if (entry instanceof MethodEntry methodEntry) {
                this.setIcon(GuiUtil.getMethodIcon(methodEntry));
            } else if (entry instanceof FieldEntry) {
                this.setIcon(GuiUtil.FIELD_ICON);
            }

            this.setText("<html>" + ((StructureTreeNode) value).toHtml());

            return c;
        }
    }
}
