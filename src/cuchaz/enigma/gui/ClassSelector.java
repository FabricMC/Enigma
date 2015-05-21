/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import cuchaz.enigma.mapping.ClassEntry;

public class ClassSelector extends JTree {
	
	private static final long serialVersionUID = -7632046902384775977L;
	
	public interface ClassSelectionListener {
		void onSelectClass(ClassEntry classEntry);
	}
	
	public static Comparator<ClassEntry> ObfuscatedClassEntryComparator;
	public static Comparator<ClassEntry> DeobfuscatedClassEntryComparator;
	
	static {
		ObfuscatedClassEntryComparator = new Comparator<ClassEntry>() {
			@Override
			public int compare(ClassEntry a, ClassEntry b) {
				String aname = a.getName();
				String bname = a.getName();
				if (aname.length() != bname.length()) {
					return aname.length() - bname.length();
				}
				return aname.compareTo(bname);
			}
		};
		
		DeobfuscatedClassEntryComparator = new Comparator<ClassEntry>() {
			@Override
			public int compare(ClassEntry a, ClassEntry b) {
				if (a instanceof ScoredClassEntry && b instanceof ScoredClassEntry) {
					return Float.compare(
						((ScoredClassEntry)b).getScore(),
						((ScoredClassEntry)a).getScore()
					);
				}
				return a.getName().compareTo(b.getName());
			}
		};
	}
	
	private ClassSelectionListener m_listener;
	private Comparator<ClassEntry> m_comparator;
	
	public ClassSelector(Comparator<ClassEntry> comparator) {
		m_comparator = comparator;
		
		// configure the tree control
		setRootVisible(false);
		setShowsRootHandles(false);
		setModel(null);
		
		// hook events
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (m_listener != null && event.getClickCount() == 2) {
					// get the selected node
					TreePath path = getSelectionPath();
					if (path != null && path.getLastPathComponent() instanceof ClassSelectorClassNode) {
						ClassSelectorClassNode node = (ClassSelectorClassNode)path.getLastPathComponent();
						m_listener.onSelectClass(node.getClassEntry());
					}
				}
			}
		});
		
		// init defaults
		m_listener = null;
	}
	
	public void setListener(ClassSelectionListener val) {
		m_listener = val;
	}
	
	public void setClasses(Collection<ClassEntry> classEntries) {
		if (classEntries == null) {
			setModel(null);
			return;
		}
		
		// build the package names
		Map<String,ClassSelectorPackageNode> packages = Maps.newHashMap();
		for (ClassEntry classEntry : classEntries) {
			packages.put(classEntry.getPackageName(), null);
		}
		
		// sort the packages
		List<String> sortedPackageNames = Lists.newArrayList(packages.keySet());
		Collections.sort(sortedPackageNames, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
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
		Multimap<String,ClassEntry> packagedClassEntries = ArrayListMultimap.create();
		for (ClassEntry classEntry : classEntries) {
			packagedClassEntries.put(classEntry.getPackageName(), classEntry);
		}
		
		// build the class nodes
		for (String packageName : packagedClassEntries.keySet()) {
			// sort the class entries
			List<ClassEntry> classEntriesInPackage = Lists.newArrayList(packagedClassEntries.get(packageName));
			Collections.sort(classEntriesInPackage, m_comparator);
			
			// create the nodes in order
			for (ClassEntry classEntry : classEntriesInPackage) {
				ClassSelectorPackageNode node = packages.get(packageName);
				node.add(new ClassSelectorClassNode(classEntry));
			}
		}
		
		// finally, update the tree control
		setModel(new DefaultTreeModel(root));
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
