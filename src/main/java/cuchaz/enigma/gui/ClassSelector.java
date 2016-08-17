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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.mapping.ClassEntry;

public class ClassSelector extends JTree {

    public static final Comparator<ClassEntry> DEOBF_CLASS_COMPARATOR = (a, b) -> a.getName().compareTo(b.getName());

    public interface ClassSelectionListener {
        void onSelectClass(ClassEntry classEntry);
    }

    private ClassSelectionListener listener;
    private Comparator<ClassEntry> comparator;

    public ClassSelector(Comparator<ClassEntry> comparator) {
        this.comparator = comparator;

        // configure the tree control
        setRootVisible(false);
        setShowsRootHandles(false);
        setModel(null);

        // hook events
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (listener != null && event.getClickCount() == 2) {
                    // get the selected node
                    TreePath path = getSelectionPath();
                    if (path != null && path.getLastPathComponent() instanceof ClassSelectorClassNode) {
                        ClassSelectorClassNode node = (ClassSelectorClassNode) path.getLastPathComponent();
                        listener.onSelectClass(node.getClassEntry());
                    }
                }
            }
        });

        // init defaults
        this.listener = null;
    }

    public void setListener(ClassSelectionListener val) {
        this.listener = val;
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

        // create the root node and the package nodes
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (String packageName : sortedPackageNames) {
            ClassSelectorPackageNode node = new ClassSelectorPackageNode(packageName);
            packages.put(packageName, node);
            root.add(node);
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
        setModel(new DefaultTreeModel(root));

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
                    buf.append("," + String.valueOf(i - row));
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

    public Iterable<ClassSelectorPackageNode> packageNodes() {
        List<ClassSelectorPackageNode> nodes = Lists.newArrayList();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)getModel().getRoot();
        Enumeration<?> children = root.children();
        while (children.hasMoreElements()) {
            ClassSelectorPackageNode packageNode = (ClassSelectorPackageNode)children.nextElement();
            nodes.add(packageNode);
        }
        return nodes;
    }

    public Iterable<ClassSelectorClassNode> classNodes(ClassSelectorPackageNode packageNode) {
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
}
