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
}
