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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.throwables.IllegalNameException;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class ClassSelector extends JTree {

    public static final Comparator<ClassEntry> DEOBF_CLASS_COMPARATOR = (a, b) -> a.getName().compareTo(b.getName());
    private DefaultMutableTreeNode rootNodes;

    public interface ClassSelectionListener {
        void onSelectClass(ClassEntry classEntry);
    }

    public interface RenameSelectionListener {
        void onSelectionRename(Object prevData, Object data, DefaultMutableTreeNode node);
    }

    private ClassSelectionListener selectionListener;
    private RenameSelectionListener renameSelectionListener;
    private Comparator<ClassEntry> comparator;

    public ClassSelector(Gui gui, Comparator<ClassEntry> comparator, boolean isRenamable) {
        this.comparator = comparator;

        // configure the tree control
        setEditable(gui != null);
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
                        selectionListener.onSelectClass(node.getClassEntry());
                    }
                }
            }
        });

        if (gui != null)
        {
            final JTree tree = this;

            final DefaultTreeCellEditor editor = new DefaultTreeCellEditor(tree,
                    (DefaultTreeCellRenderer) tree.getCellRenderer())
            {
                @Override public boolean isCellEditable(EventObject event)
                {
                    return isRenamable && !(event instanceof MouseEvent) && super.isCellEditable(event);
                }
            };
            this.setCellEditor(editor);
            editor.addCellEditorListener(new CellEditorListener()
            {
                @Override public void editingStopped(ChangeEvent e)
                {
                    String data = editor.getCellEditorValue().toString();
                    TreePath path = getSelectionPath();

                    Object realPath = path.getLastPathComponent();
                    if (realPath != null && realPath instanceof DefaultMutableTreeNode && data != null)
                    {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) realPath;
                        TreeNode parentNode = node.getParent();
                        if (parentNode == null)
                            return;
                        boolean allowEdit = true;
                        for (int i = 0; i < parentNode.getChildCount(); i++)
                        {
                            TreeNode childNode = parentNode.getChildAt(i);
                            if (childNode != null && childNode.toString().equals(data) && childNode != node)
                            {
                                allowEdit = false;
                                break;
                            }
                        }
                        if (allowEdit && renameSelectionListener != null)
                        {
                            Object prevData = node.getUserObject();
                            Object objectData = node.getUserObject() instanceof ClassEntry ? new ClassEntry(((ClassEntry)prevData).getPackageName() + "/" + data) : data;
                            try
                            {
                                renameSelectionListener.onSelectionRename(node.getUserObject(), objectData, node);
                                node.setUserObject(objectData); // Make sure that it's modified
                            } catch (IllegalNameException ex)
                            {
                                JOptionPane.showOptionDialog(gui.getFrame(), ex.getMessage(), "Enigma - Error", JOptionPane.OK_OPTION,
                                        JOptionPane.ERROR_MESSAGE, null, new String[] {"Ok"}, "OK");
                                editor.cancelCellEditing();
                            }
                        }
                        else
                            editor.cancelCellEditing();
                    }

                }

                @Override public void editingCanceled(ChangeEvent e)
                {
                    // NOP
                }
            });
        }
        // init defaults
        this.selectionListener = null;
        this.renameSelectionListener = null;
    }

    public boolean isDuplicate(Object[] nodes, String data)
    {
        int count = 0;

        for (Object node : nodes)
        {
            if (node.toString().equals(data))
            {
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

    public void setRenameSelectionListener(RenameSelectionListener renameSelectionListener)
    {
        this.renameSelectionListener = renameSelectionListener;
    }

    public void setClasses(Collection<ClassEntry> classEntries) {
        String state = getExpansionState(this, 0);
        if (classEntries == null) {
            setModel(null);
            return;
        }

        // build the package names
        Map<String, ClassSelectorPackageNode> packages = Maps.newHashMap();
        for (ClassEntry classEntry : classEntries) {
            packages.put(classEntry.getPackageName(), null);
        }

        // sort the packages
        List<String> sortedPackageNames = Lists.newArrayList(packages.keySet());
        Collections.sort(sortedPackageNames, (a, b) -> {
            // I can never keep this rule straight when writing these damn things...
            // a < b => -1, a == b => 0, a > b => +1

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
        for (ClassEntry classEntry : classEntries) {
            packagedClassEntries.put(classEntry.getPackageName(), classEntry);
        }

        // build the class nodes
        for (String packageName : packagedClassEntries.keySet()) {
            // sort the class entries
            List<ClassEntry> classEntriesInPackage = Lists.newArrayList(packagedClassEntries.get(packageName));
            Collections.sort(classEntriesInPackage, this.comparator);

            // create the nodes in order
            for (ClassEntry classEntry : classEntriesInPackage) {
                ClassSelectorPackageNode node = packages.get(packageName);
                node.add(new ClassSelectorClassNode(classEntry));
            }
        }

        // finally, update the tree control
        setModel(new DefaultTreeModel(rootNodes));

        restoreExpanstionState(this, 0, state);
    }

    public ClassEntry getSelectedClass() {
        if (!isSelectionEmpty()) {
            Object selectedNode = getSelectionPath().getLastPathComponent();
            if (selectedNode instanceof ClassSelectorClassNode) {
                ClassSelectorClassNode classNode = (ClassSelectorClassNode)selectedNode;
                return classNode.getClassEntry();
            }
        }
        return null;
    }

    public String getSelectedPackage() {
        if (!isSelectionEmpty()) {
            Object selectedNode = getSelectionPath().getLastPathComponent();
            if (selectedNode instanceof ClassSelectorPackageNode) {
                ClassSelectorPackageNode packageNode = (ClassSelectorPackageNode)selectedNode;
                return packageNode.getPackageName();
            } else if (selectedNode instanceof ClassSelectorClassNode) {
                ClassSelectorClassNode classNode = (ClassSelectorClassNode)selectedNode;
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

    public String getExpansionState(JTree tree, int row) {
        TreePath rowPath = tree.getPathForRow(row);
        StringBuffer buf = new StringBuffer();
        int rowCount = tree.getRowCount();
        for (int i = row; i < rowCount; i++) {
            TreePath path = tree.getPathForRow(i);
            if (i == row || isDescendant(path, rowPath)) {
                if (tree.isExpanded(path)) {
                    buf.append(",").append(String.valueOf(i - row));
                }
            } else {
                break;
            }
        }
        return buf.toString();
    }

    public void restoreExpanstionState(JTree tree, int row, String expansionState) {
        StringTokenizer stok = new StringTokenizer(expansionState, ",");
        while (stok.hasMoreTokens()) {
            int token = row + Integer.parseInt(stok.nextToken());
            tree.expandRow(token);
        }
    }

    public List<ClassSelectorPackageNode> packageNodes() {
        List<ClassSelectorPackageNode> nodes = Lists.newArrayList();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)getModel().getRoot();
        Enumeration<?> children = root.children();
        while (children.hasMoreElements()) {
            ClassSelectorPackageNode packageNode = (ClassSelectorPackageNode)children.nextElement();
            nodes.add(packageNode);
        }
        return nodes;
    }

    public List<ClassSelectorClassNode> classNodes(ClassSelectorPackageNode packageNode) {
        List<ClassSelectorClassNode> nodes = Lists.newArrayList();
        Enumeration<?> children = packageNode.children();
        while (children.hasMoreElements()) {
            ClassSelectorClassNode classNode = (ClassSelectorClassNode)children.nextElement();
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
                expandPath(new TreePath(new Object[] {getModel().getRoot(), packageNode}));
                return;
            }
        }
    }

    public void expandAll() {
        for (ClassSelectorPackageNode packageNode : packageNodes()) {
            expandPath(new TreePath(new Object[] {getModel().getRoot(), packageNode}));
        }
    }

    public ClassEntry getFirstClass() {
        for (ClassSelectorPackageNode packageNode : packageNodes()) {
            for (ClassSelectorClassNode classNode : classNodes(packageNode)) {
                return classNode.getClassEntry();
            }
        }
        return null;
    }

    public ClassSelectorPackageNode getPackageNode(ClassEntry entry) {
        for (ClassSelectorPackageNode packageNode : packageNodes()) {
            if (packageNode.getPackageName().equals(entry.getPackageName())) {
                return packageNode;
            }
        }
        return null;
    }

    public ClassSelectorPackageNode getPackageNode(ClassSelector selector, ClassEntry entry)
    {
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
                for (ClassSelectorClassNode classNode : classNodes(packageNode)) {
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
                    setSelectionPath(new TreePath(new Object[] {getModel().getRoot(), packageNode, classNode}));
                }
            }
        }
    }

    public void removeNode(ClassSelectorPackageNode packageNode, ClassEntry entry)
    {
        DefaultTreeModel model = (DefaultTreeModel) getModel();

        if (packageNode == null)
            return;

        for (int i = 0; i < packageNode.getChildCount(); i++)
        {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) packageNode.getChildAt(i);
            if (childNode.getUserObject() instanceof ClassEntry && childNode.getUserObject().equals(entry))
            {
                model.removeNodeFromParent(childNode);
                break;
            }
        }
    }

    public void removeNodeIfEmpty(ClassSelectorPackageNode packageNode)
    {
        if (packageNode != null && packageNode.getChildCount() == 0)
            ((DefaultTreeModel) getModel()).removeNodeFromParent(packageNode);
    }

    public void moveClassTree(ClassEntry oldClassEntry, ClassEntry newClassEntry, ClassSelector otherSelector)
    {
        if (otherSelector == null)
            removeNode(getPackageNode(oldClassEntry), oldClassEntry);
        insertNode(getOrCreate(newClassEntry), newClassEntry);
    }

    public ClassSelectorPackageNode getOrCreate(ClassEntry entry)
    {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        ClassSelectorPackageNode newPackageNode = getPackageNode(entry);
        if (newPackageNode == null)
        {
            newPackageNode = new ClassSelectorPackageNode(entry.getPackageName());
            model.insertNodeInto(newPackageNode, (MutableTreeNode) model.getRoot(), getPlacementIndex(newPackageNode));
        }
        return newPackageNode;
    }

    public void insertNode(ClassSelectorPackageNode packageNode, ClassEntry entry)
    {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        ClassSelectorClassNode classNode = new ClassSelectorClassNode(entry);
        model.insertNodeInto(classNode, packageNode, getPlacementIndex(packageNode, classNode));
    }

    public void reload()
    {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.reload(sort(rootNodes));
    }

    private DefaultMutableTreeNode sort(DefaultMutableTreeNode node) {

        for(int i = 0; i < node.getChildCount() - 1; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child == null)
                continue;
            String nt = child.toString();

            for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
                DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
                if (prevNode == null || prevNode.getUserObject() == null)
                    continue;
                String np = prevNode.getUserObject().toString();

                if(nt.compareToIgnoreCase(np) > 0) {
                    node.insert(child, j);
                    node.insert(prevNode, i);
                }
            }
            if(child.getChildCount() > 0) {
                sort(child);
            }
        }

        for(int i = 0; i < node.getChildCount() - 1; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
                DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);

                if(!prevNode.isLeaf() && child.isLeaf()) {
                    node.insert(child, j);
                    node.insert(prevNode, i);
                }
            }
        }

        return node;
    }

    private int getPlacementIndex(ClassSelectorPackageNode newPackageNode, ClassSelectorClassNode classNode)
    {
        List<ClassSelectorClassNode> classNodes = classNodes(newPackageNode);
        classNodes.add(classNode);
        Collections.sort(classNodes, (a, b) -> a.toString().compareTo(b.toString()));
        for (int i = 0; i < classNodes.size(); i++)
            if (classNodes.get(i) == classNode)
                return i;

        return 0;
    }

    private int getPlacementIndex(ClassSelectorPackageNode newPackageNode)
    {
        List<ClassSelectorPackageNode> packageNodes = packageNodes();
        if (!packageNodes.contains(newPackageNode))
        {
            packageNodes.add(newPackageNode);
            Collections.sort(packageNodes, (a, b) -> a.toString().compareTo(b.toString()));
        }

        for (int i = 0; i < packageNodes.size(); i++)
            if (packageNodes.get(i) == newPackageNode)
                return i;

        return 0;
    }
}
