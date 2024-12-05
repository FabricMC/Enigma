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

package cuchaz.enigma.gui;

import cuchaz.enigma.gui.node.ClassSelectorPackageNode;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.validation.ValidationContext;

public class ClassSelector extends JTree {
	public static final Comparator<ClassEntry> DEOBF_CLASS_COMPARATOR = Comparator.comparing(ClassEntry::getFullName);

	private final Comparator<ClassEntry> comparator;
	private final GuiController controller;

	private NestedPackages packageManager;
	private ClassSelectionListener selectionListener;
	private RenameSelectionListener renameSelectionListener;

	public ClassSelector(Gui gui, Comparator<ClassEntry> comparator, boolean isRenamable) {
		this.comparator = comparator;
		this.controller = gui.getController();

		// configure the tree control
		setEditable(true);
		setRootVisible(false);
		setShowsRootHandles(false);
		setModel(null);

		// hook events
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (selectionListener != null && event.getClickCount() == 2) {
					// get the selected node
					TreePath path = getSelectionPath();

					if (path != null && path.getLastPathComponent() instanceof ClassSelectorClassNode node) {
						selectionListener.onSelectClass(node.getObfEntry());
					}
				}
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				TreePath[] paths = getSelectionPaths();

				if (paths != null) {
					if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_O) {
						for (TreePath path : paths) {
							if (path.getLastPathComponent() instanceof ClassSelectorClassNode node) {
								gui.toggleMappingFromEntry(node.getObfEntry());
							}
						}
					}

					if (selectionListener != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
						for (TreePath path : paths) {
							if (path.getLastPathComponent() instanceof ClassSelectorClassNode node) {
								selectionListener.onSelectClass(node.getObfEntry());
							}
						}
					}
				}
			}
		});

		final DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
			{
				setLeafIcon(GuiUtil.CLASS_ICON);
			}

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

				if (leaf && value instanceof ClassSelectorClassNode) {
					setIcon(GuiUtil.getClassIcon(gui, ((ClassSelectorClassNode) value).getObfEntry()));
				}

				return this;
			}
		};
		setCellRenderer(renderer);

		final JTree tree = this;

		final DefaultTreeCellEditor editor = new DefaultTreeCellEditor(tree, renderer) {
			@Override
			public boolean isCellEditable(EventObject event) {
				return isRenamable && !(event instanceof MouseEvent) && super.isCellEditable(event);
			}
		};
		this.setCellEditor(editor);
		editor.addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent e) {
				String data = editor.getCellEditorValue().toString();
				TreePath path = getSelectionPath();

				Object realPath = path.getLastPathComponent();

				if (realPath instanceof DefaultMutableTreeNode node && data != null) {
					TreeNode parentNode = node.getParent();

					if (parentNode == null) {
						return;
					}

					boolean allowEdit = true;

					for (int i = 0; i < parentNode.getChildCount(); i++) {
						TreeNode childNode = parentNode.getChildAt(i);

						if (childNode != null && childNode.toString().equals(data) && childNode != node) {
							allowEdit = false;
							break;
						}
					}

					if (allowEdit && renameSelectionListener != null) {
						gui.validateImmediateAction(vc -> {
							String parentName;

							if(node instanceof ClassSelectorPackageNode packageNode) {
								String packageName = packageNode.getPackageName();
								int lastSlash = packageName.lastIndexOf("/");
								if(lastSlash != -1) {
									parentName = packageName.substring(0, lastSlash);
								} else {
									parentName = "";
								}
							} else if(node instanceof ClassSelectorClassNode classNode) {
								parentName = classNode.getClassEntry().getPackageName();
							} else {
								throw new IllegalStateException("unknown node type " + node.getClass().getSimpleName());
							}

							renameSelectionListener.onSelectionRename(vc, parentName + "/" + data, node);

							if (!vc.canProceed()) {
								editor.cancelCellEditing();
							}
						});
					} else {
						editor.cancelCellEditing();
					}
				}
			}

			@Override
			public void editingCanceled(ChangeEvent e) {
				// NOP
			}
		});
		// init defaults
		this.selectionListener = null;
		this.renameSelectionListener = null;
	}

	public void setSelectionListener(ClassSelectionListener val) {
		this.selectionListener = val;
	}

	public void setRenameSelectionListener(RenameSelectionListener renameSelectionListener) {
		this.renameSelectionListener = renameSelectionListener;
	}

	public void setClasses(Collection<ClassEntry> classEntries) {
		List<StateEntry> state = getExpansionState();

		if (classEntries == null) {
			setModel(null);
			return;
		}

		// update the tree control
		packageManager = new NestedPackages(classEntries, comparator, controller.project.getMapper());
		setModel(new DefaultTreeModel(packageManager.getRoot()));

		restoreExpansionState(state);
	}

	public ClassEntry getSelectedClass() {
		if (!isSelectionEmpty()) {
			Object selectedNode = getSelectionPath().getLastPathComponent();

			if (selectedNode instanceof ClassSelectorClassNode classNode) {
				return classNode.getClassEntry();
			}
		}

		return null;
	}

	public enum State {
		EXPANDED,
		SELECTED
	}

	public record StateEntry(State state, TreePath path) {
	}

	public List<StateEntry> getExpansionState() {
		List<StateEntry> state = new ArrayList<>();
		int rowCount = getRowCount();

		for (int i = 0; i < rowCount; i++) {
			TreePath path = getPathForRow(i);

			if (isPathSelected(path)) {
				state.add(new StateEntry(State.SELECTED, path));
			}

			if (isExpanded(path)) {
				state.add(new StateEntry(State.EXPANDED, path));
			}
		}

		return state;
	}

	public void restoreExpansionState(List<StateEntry> expansionState) {
		clearSelection();

		for (StateEntry entry : expansionState) {
			switch (entry.state) {
			case SELECTED -> addSelectionPath(entry.path);
			case EXPANDED -> expandPath(entry.path);
			}
		}
	}

	public void expandPackage(String packageName) {
		if (packageName == null) {
			return;
		}

		expandPath(packageManager.getPackagePath(packageName));
	}

	public void expandAll() {
		for (DefaultMutableTreeNode packageNode : packageManager.getPackageNodes()) {
			expandPath(new TreePath(packageNode.getPath()));
		}
	}

	public void collapseAll() {
		// sort the package nodes by depth, so we collapse the deepest nodes first
		List<DefaultMutableTreeNode> packageNodes = new ArrayList<>(packageManager.getPackageNodes());
		packageNodes.sort(Comparator.comparingInt(DefaultMutableTreeNode::getDepth));

		// collapse the nodes
		for (DefaultMutableTreeNode packageNode : packageNodes) {
			collapsePath(new TreePath(packageNode.getPath()));
		}
	}

	public void setSelectionClass(ClassEntry classEntry) {
		expandPackage(classEntry.getPackageName());
		ClassSelectorClassNode node = packageManager.getClassNode(classEntry);

		if (node != null) {
			TreePath path = new TreePath(node.getPath());
			setSelectionPath(path);
			scrollPathToVisible(path);
		}
	}

	public void moveClassIn(ClassEntry classEntry) {
		removeEntry(classEntry);
		packageManager.addEntry(classEntry);
	}

	public void removeEntry(ClassEntry classEntry) {
		packageManager.removeClassNode(classEntry);
	}

	public void reload() {
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		model.reload(packageManager.getRoot());
	}

	public interface ClassSelectionListener {
		void onSelectClass(ClassEntry classEntry);
	}

	public interface RenameSelectionListener {
		void onSelectionRename(ValidationContext vc, String targetName, DefaultMutableTreeNode node);
	}
}
