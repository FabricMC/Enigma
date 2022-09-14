package cuchaz.enigma.gui.elements;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

public abstract class AbstractInheritanceTree {
	private final JPanel panel = new JPanel(new BorderLayout());

	private final JTree tree = new JTree();

	protected final Gui gui;

	public AbstractInheritanceTree(Gui gui, TreeCellRenderer cellRenderer) {
		this.gui = gui;

		this.tree.setModel(null);
		this.tree.setCellRenderer(cellRenderer);
		this.tree.setSelectionModel(new SingleTreeSelectionModel());
		this.tree.setShowsRootHandles(true);
		this.tree.addMouseListener(GuiUtil.onMouseClick(this::onClick));

		this.panel.add(new JScrollPane(this.tree));
	}

	private void onClick(MouseEvent event) {
		if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
			// get the selected node
			TreePath path = tree.getSelectionPath();

			if (path == null) {
				return;
			}

			Object node = path.getLastPathComponent();

			if (node instanceof ClassInheritanceTreeNode classNode) {
				gui.getController().navigateTo(new ClassEntry(classNode.getObfClassName()));
			} else if (node instanceof MethodInheritanceTreeNode methodNode) {
				if (methodNode.isImplemented()) {
					gui.getController().navigateTo(methodNode.getMethodEntry());
				}
			}
		}
	}

	public void display(Entry<?> entry) {
		this.tree.setModel(null);

		DefaultMutableTreeNode node = this.getNodeFor(entry);

		if (node != null) {
			// show the tree at the root
			TreePath path = GuiUtil.getPathToRoot(node);
			this.tree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			this.tree.expandPath(path);
			this.tree.setSelectionRow(this.tree.getRowForPath(path));
		}

		this.panel.show();
	}

	public void retranslateUi() {
	}

	@Nullable
	protected abstract DefaultMutableTreeNode getNodeFor(Entry<?> entry);

	protected abstract String getPanelName();

	public JPanel getPanel() {
		return this.panel;
	}
}
