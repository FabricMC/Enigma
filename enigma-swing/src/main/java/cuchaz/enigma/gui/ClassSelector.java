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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.translation.mapping.IllegalNameException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class ClassSelector extends JTree {

	public static final Comparator<ClassEntry> DEOBF_CLASS_COMPARATOR = Comparator.comparing(ClassEntry::getFullName);

	private final GuiController controller;

	private DefaultMutableTreeNode rootNodes;
	private ClassSelectionListener selectionListener;
	private RenameSelectionListener renameSelectionListener;
	private Comparator<ClassEntry> comparator;

	private final Map<ClassEntry, ClassEntry> displayedObfToDeobf = new HashMap<>();

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
					if (path != null && path.getLastPathComponent() instanceof ClassSelectorClassNode) {
						ClassSelectorClassNode node = (ClassSelectorClassNode) path.getLastPathComponent();
						selectionListener.onSelectClass(node.getObfEntry());
					}
				}
			}
		});

		final JTree tree = this;

		final DefaultTreeCellEditor editor = new DefaultTreeCellEditor(tree,
				(DefaultTreeCellRenderer) tree.getCellRenderer()) {
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
				if (realPath != null && realPath instanceof DefaultMutableTreeNode && data != null) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) realPath;
					TreeNode parentNode = node.getParent();
					if (parentNode == null)
						return;
					boolean allowEdit = true;
					for (int i = 0; i < parentNode.getChildCount(); i++) {
						TreeNode childNode = parentNode.getChildAt(i);
						if (childNode != null && childNode.toString().equals(data) && childNode != node) {
							allowEdit = false;
							break;
						}
					}
					if (allowEdit && renameSelectionListener != null) {
						Object prevData = node.getUserObject();
						Object objectData = node.getUserObject() instanceof ClassEntry ? new ClassEntry(((ClassEntry) prevData).getPackageName() + "/" + data) : data;
						try {
							renameSelectionListener.onSelectionRename(node.getUserObject(), objectData, node);
							node.setUserObject(objectData); // Make sure that it's modified
						} catch (IllegalNameException ex) {
							JOptionPane.showOptionDialog(gui.getFrame(), ex.getMessage(), "Enigma - Error", JOptionPane.OK_OPTION,
									JOptionPane.ERROR_MESSAGE, null, new String[]{"Ok"}, "OK");
							editor.cancelCellEditing();
						}
					} else
						editor.cancelCellEditing();
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

	public boolean isDuplicate(Object[] nodes, String data) {
		int count = 0;

		for (Object node : nodes) {
			if (node.toString().equals(data)) {
				count++;
				if (count == 2)
					return true;
			}
		}
		return false;
	}

	public void setSelectionListener(ClassSelectionListener val) {
		this.selectionListener = val;
	}

	public void setRenameSelectionListener(RenameSelectionListener renameSelectionListener) {
		this.renameSelectionListener = renameSelectionListener;
	}

	public void setClasses(Collection<ClassEntry> classEntries) {
		displayedObfToDeobf.clear();

		List<StateEntry> state = getExpansionState(this);
		if (classEntries == null) {
			setModel(null);
			return;
		}

		Translator translator = controller.project.getMapper().getDeobfuscator();

		// build the package names
		Map<String, ClassSelectorPackageNode> packages = Maps.newHashMap();
		for (ClassEntry obfClass : classEntries) {
			ClassEntry deobfClass = translator.translate(obfClass);
			packages.put(deobfClass.getPackageName(), null);
		}

		// sort the packages
		List<String> sortedPackageNames = Lists.newArrayList(packages.keySet());
		sortedPackageNames.sort((a, b) ->
		{
			// I can never keep this rule straight when writing these damn things...
			// a < b => -1, a == b => 0, a > b => +1

			if (b == null || a == null) {
				return 0;
			}

			String[] aparts = a.split("/");
			String[] bparts = b.split("/");
			for (int i = 0; true; i++) {
				if (i >= aparts.length) {
					return -1;
				} else if (i >= bparts.length) {
					return 1;
				}

				int result = aparts[i].compareTo(bparts[i]);
				if (result != 0) {
					return result;
				}
			}
		});

		// create the rootNodes node and the package nodes
		rootNodes = new DefaultMutableTreeNode();
		for (String packageName : sortedPackageNames) {
			ClassSelectorPackageNode node = new ClassSelectorPackageNode(packageName);
			packages.put(packageName, node);
			rootNodes.add(node);
		}

		// put the classes into packages
		Multimap<String, ClassEntry> packagedClassEntries = ArrayListMultimap.create();
		for (ClassEntry obfClass : classEntries) {
			ClassEntry deobfClass = translator.translate(obfClass);
			packagedClassEntries.put(deobfClass.getPackageName(), obfClass);
		}

		// build the class nodes
		for (String packageName : packagedClassEntries.keySet()) {
			// sort the class entries
			List<ClassEntry> classEntriesInPackage = Lists.newArrayList(packagedClassEntries.get(packageName));
			classEntriesInPackage.sort((o1, o2) -> comparator.compare(translator.translate(o1), translator.translate(o2)));

			// create the nodes in order
			for (ClassEntry obfClass : classEntriesInPackage) {
				ClassEntry deobfClass = translator.translate(obfClass);
				ClassSelectorPackageNode node = packages.get(packageName);
				ClassSelectorClassNode classNode = new ClassSelectorClassNode(obfClass, deobfClass);
				displayedObfToDeobf.put(obfClass, deobfClass);
				node.add(classNode);
			}
		}

		// finally, update the tree control
		setModel(new DefaultTreeModel(rootNodes));

		restoreExpansionState(this, state);
	}

	public ClassEntry getSelectedClass() {
		if (!isSelectionEmpty()) {
			Object selectedNode = getSelectionPath().getLastPathComponent();
			if (selectedNode instanceof ClassSelectorClassNode) {
				ClassSelectorClassNode classNode = (ClassSelectorClassNode) selectedNode;
				return classNode.getClassEntry();
			}
		}
		return null;
	}

	public String getSelectedPackage() {
		if (!isSelectionEmpty()) {
			Object selectedNode = getSelectionPath().getLastPathComponent();
			if (selectedNode instanceof ClassSelectorPackageNode) {
				ClassSelectorPackageNode packageNode = (ClassSelectorPackageNode) selectedNode;
				return packageNode.getPackageName();
			} else if (selectedNode instanceof ClassSelectorClassNode) {
				ClassSelectorClassNode classNode = (ClassSelectorClassNode) selectedNode;
				return classNode.getClassEntry().getPackageName();
			}
		}
		return null;
	}

	public boolean isDescendant(TreePath path1, TreePath path2) {
		int count1 = path1.getPathCount();
		int count2 = path2.getPathCount();
		if (count1 <= count2) {
			return false;
		}
		while (count1 != count2) {
			path1 = path1.getParentPath();
			count1--;
		}
		return path1.equals(path2);
	}

	public enum State {
		EXPANDED,
		SELECTED
	}

	public static class StateEntry {
		public final State state;
		public final TreePath path;

		public StateEntry(State state, TreePath path) {
			this.state = state;
			this.path = path;
		}
	}

	public List<StateEntry> getExpansionState(JTree tree) {
		List<StateEntry> state = new ArrayList<>();
		int rowCount = tree.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			TreePath path = tree.getPathForRow(i);
			if (tree.isPathSelected(path)) {
				state.add(new StateEntry(State.SELECTED, path));
			}
			if (tree.isExpanded(path)) {
				state.add(new StateEntry(State.EXPANDED, path));
			}
		}
		return state;
	}

	public void restoreExpansionState(JTree tree, List<StateEntry> expansionState) {
		tree.clearSelection();

		for (StateEntry entry : expansionState) {
			switch (entry.state) {
				case SELECTED:
					tree.addSelectionPath(entry.path);
					break;
				case EXPANDED:
					tree.expandPath(entry.path);
					break;
			}
		}
	}

	public List<ClassSelectorPackageNode> packageNodes() {
		List<ClassSelectorPackageNode> nodes = Lists.newArrayList();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
		Enumeration<?> children = root.children();
		while (children.hasMoreElements()) {
			ClassSelectorPackageNode packageNode = (ClassSelectorPackageNode) children.nextElement();
			nodes.add(packageNode);
		}
		return nodes;
	}

	public List<ClassSelectorClassNode> classNodes(ClassSelectorPackageNode packageNode) {
		List<ClassSelectorClassNode> nodes = Lists.newArrayList();
		Enumeration<?> children = packageNode.children();
		while (children.hasMoreElements()) {
			ClassSelectorClassNode classNode = (ClassSelectorClassNode) children.nextElement();
			nodes.add(classNode);
		}
		return nodes;
	}

	public void expandPackage(String packageName) {
		if (packageName == null) {
			return;
		}
		for (ClassSelectorPackageNode packageNode : packageNodes()) {
			if (packageNode.getPackageName().equals(packageName)) {
				expandPath(new TreePath(new Object[]{getModel().getRoot(), packageNode}));
				return;
			}
		}
	}

	public void expandAll() {
		for (ClassSelectorPackageNode packageNode : packageNodes()) {
			expandPath(new TreePath(new Object[]{getModel().getRoot(), packageNode}));
		}
	}

	public ClassEntry getFirstClass() {
		ClassSelectorPackageNode packageNode = packageNodes().get(0);
		if (packageNode != null) {
			ClassSelectorClassNode classNode = classNodes(packageNode).get(0);
			if (classNode != null) {
				return classNode.getClassEntry();
			}
		}
		return null;
	}

	public ClassSelectorPackageNode getPackageNode(ClassEntry entry) {
		String packageName = entry.getPackageName();
		if (packageName == null) {
			packageName = "(none)";
		}
		for (ClassSelectorPackageNode packageNode : packageNodes()) {
			if (packageNode.getPackageName().equals(packageName)) {
				return packageNode;
			}
		}
		return null;
	}

	@Nullable
	public ClassEntry getDisplayedDeobf(ClassEntry obfEntry) {
		return displayedObfToDeobf.get(obfEntry);
	}

	public ClassSelectorPackageNode getPackageNode(ClassSelector selector, ClassEntry entry) {
		ClassSelectorPackageNode packageNode = getPackageNode(entry);

		if (selector != null && packageNode == null && selector.getPackageNode(entry) != null)
			return selector.getPackageNode(entry);
		return packageNode;
	}

	public ClassEntry getNextClass(ClassEntry entry) {
		boolean foundIt = false;
		for (ClassSelectorPackageNode packageNode : packageNodes()) {
			if (!foundIt) {
				// skip to the package with our target in it
				if (packageNode.getPackageName().equals(entry.getPackageName())) {
					for (ClassSelectorClassNode classNode : classNodes(packageNode)) {
						if (!foundIt) {
							if (classNode.getClassEntry().equals(entry)) {
								foundIt = true;
							}
						} else {
							// return the next class
							return classNode.getClassEntry();
						}
					}
				}
			} else {
				// return the next class
				ClassSelectorClassNode classNode = classNodes(packageNode).get(0);
				if (classNode != null) {
					return classNode.getClassEntry();
				}
			}
		}
		return null;
	}

	public void setSelectionClass(ClassEntry classEntry) {
		expandPackage(classEntry.getPackageName());
		for (ClassSelectorPackageNode packageNode : packageNodes()) {
			for (ClassSelectorClassNode classNode : classNodes(packageNode)) {
				if (classNode.getClassEntry().equals(classEntry)) {
					TreePath path = new TreePath(new Object[]{getModel().getRoot(), packageNode, classNode});
					setSelectionPath(path);
					scrollPathToVisible(path);
				}
			}
		}
	}

	public void removeNode(ClassSelectorPackageNode packageNode, ClassEntry entry) {
		DefaultTreeModel model = (DefaultTreeModel) getModel();

		if (packageNode == null)
			return;

		for (int i = 0; i < packageNode.getChildCount(); i++) {
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) packageNode.getChildAt(i);
			if (childNode.getUserObject() instanceof ClassEntry && childNode.getUserObject().equals(entry)) {
				model.removeNodeFromParent(childNode);
				if (childNode instanceof ClassSelectorClassNode) {
					displayedObfToDeobf.remove(((ClassSelectorClassNode) childNode).getObfEntry());
				}
				break;
			}
		}
	}

	public void removeNodeIfEmpty(ClassSelectorPackageNode packageNode) {
		if (packageNode != null && packageNode.getChildCount() == 0)
			((DefaultTreeModel) getModel()).removeNodeFromParent(packageNode);
	}

	public void moveClassIn(ClassEntry classEntry) {
		removeEntry(classEntry);
		insertNode(classEntry);
	}

	public void moveClassOut(ClassEntry classEntry) {
		removeEntry(classEntry);
	}

	private void removeEntry(ClassEntry classEntry) {
		ClassEntry previousDeobf = displayedObfToDeobf.get(classEntry);
		if (previousDeobf != null) {
			ClassSelectorPackageNode packageNode = getPackageNode(previousDeobf);
			removeNode(packageNode, previousDeobf);
			removeNodeIfEmpty(packageNode);
		}
	}

	public ClassSelectorPackageNode getOrCreatePackage(ClassEntry entry) {
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		ClassSelectorPackageNode newPackageNode = getPackageNode(entry);
		if (newPackageNode == null) {
			newPackageNode = new ClassSelectorPackageNode(entry.getPackageName());
			model.insertNodeInto(newPackageNode, (MutableTreeNode) model.getRoot(), getPlacementIndex(newPackageNode));
		}
		return newPackageNode;
	}

	public void insertNode(ClassEntry obfEntry) {
		ClassEntry deobfEntry = controller.project.getMapper().deobfuscate(obfEntry);
		ClassSelectorPackageNode packageNode = getOrCreatePackage(deobfEntry);

		DefaultTreeModel model = (DefaultTreeModel) getModel();
		ClassSelectorClassNode classNode = new ClassSelectorClassNode(obfEntry, deobfEntry);
		model.insertNodeInto(classNode, packageNode, getPlacementIndex(packageNode, classNode));

		displayedObfToDeobf.put(obfEntry, deobfEntry);
	}

	public void reload() {
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		model.reload(rootNodes);
	}

	private int getPlacementIndex(ClassSelectorPackageNode newPackageNode, ClassSelectorClassNode classNode) {
		List<ClassSelectorClassNode> classNodes = classNodes(newPackageNode);
		classNodes.add(classNode);
		classNodes.sort((a, b) -> comparator.compare(a.getClassEntry(), b.getClassEntry()));
		for (int i = 0; i < classNodes.size(); i++)
			if (classNodes.get(i) == classNode)
				return i;

		return 0;
	}

	private int getPlacementIndex(ClassSelectorPackageNode newPackageNode) {
		List<ClassSelectorPackageNode> packageNodes = packageNodes();
		if (!packageNodes.contains(newPackageNode)) {
			packageNodes.add(newPackageNode);
			packageNodes.sort(Comparator.comparing(ClassSelectorPackageNode::toString));
		}

		for (int i = 0; i < packageNodes.size(); i++)
			if (packageNodes.get(i) == newPackageNode)
				return i;

		return 0;
	}

	public interface ClassSelectionListener {
		void onSelectClass(ClassEntry classEntry);
	}

	public interface RenameSelectionListener {
		void onSelectionRename(Object prevData, Object data, DefaultMutableTreeNode node);
	}
}
