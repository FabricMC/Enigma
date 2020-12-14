package cuchaz.enigma.gui.panels;

import cuchaz.enigma.analysis.StructureTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StructurePanel extends JPanel {
    private final Gui gui;

    private JPanel sortingPanel;
    private JCheckBox hideDeobfuscated;

    private JTree structureTree;

    public StructurePanel(Gui gui) {
        this.gui = gui;

        this.sortingPanel = new JPanel();
        this.hideDeobfuscated = new JCheckBox(I18n.translate("info_panel.tree.structure.hide_deobfuscated"));
        this.hideDeobfuscated.addActionListener(event -> gui.showStructure(gui.getActiveEditor()));
        this.sortingPanel.add(this.hideDeobfuscated);
        this.sortingPanel.setVisible(false);

        this.structureTree = new JTree();
        this.structureTree.setModel(null);
        this.structureTree.setCellRenderer(new StructureTreeCellRenderer());
        this.structureTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() >= 2) {
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

    public boolean shouldHideDeobfuscated() {
        return this.hideDeobfuscated.isSelected();
    }

    public JTree getStructureTree() {
        return this.structureTree;
    }

    public void retranslateUi() {
        this.hideDeobfuscated.setText(I18n.translate("info_panel.tree.structure.hide_deobfuscated"));
    }

    class StructureTreeCellRenderer implements TreeCellRenderer {
        private JLabel label;

        public StructureTreeCellRenderer() {
            this.label = new JLabel();
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            ParentedEntry entry = ((StructureTreeNode) value).getEntry();

            if (entry instanceof ClassEntry) {
                this.label.setIcon(GuiUtil.CLASS_ICON);
            } else if (entry instanceof MethodEntry) {
                this.label.setIcon(GuiUtil.METHOD_ICON);
            } else if (entry instanceof FieldEntry) {
                this.label.setIcon(GuiUtil.FIELD_ICON);
            }

            this.label.setText(value.toString());

            return this.label;
        }
    }
}
