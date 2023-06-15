package cuchaz.enigma.gui;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.gui.node.SortedMutableTreeNode;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class NestedPackages {
	private final SortedMutableTreeNode root;
	private final Map<String, SortedMutableTreeNode> packageToNode = new HashMap<>();
	private final Map<ClassEntry, ClassSelectorClassNode> classToNode = new HashMap<>();
	private final EntryRemapper remapper;
	private final Comparator<TreeNode> comparator;

	public NestedPackages(Iterable<ClassEntry> entries, Comparator<ClassEntry> entryComparator, EntryRemapper remapper) {
		this.remapper = remapper;
		this.comparator = (a, b) -> {
			if (a instanceof ClassSelectorPackageNode pA) {
				if (b instanceof ClassSelectorPackageNode pB) {
					return pA.getPackageName().compareTo(pB.getPackageName());
				} else {
					return -1;
				}
			} else if (a instanceof ClassSelectorClassNode cA) {
				if (b instanceof ClassSelectorClassNode cB) {
					return entryComparator.compare(cA.getClassEntry(), cB.getClassEntry());
				} else {
					return 1;
				}
			}

			return 0;
		};
		this.root = new SortedMutableTreeNode(comparator);

		for (ClassEntry entry : entries) {
			addEntry(entry);
		}
	}

	public void addEntry(ClassEntry entry) {
		ClassEntry translated = remapper.deobfuscate(entry);
		var me = new ClassSelectorClassNode(entry, translated);
		classToNode.put(entry, me);
		getPackage(translated.getPackageName()).insert(me, 0);
	}

	public SortedMutableTreeNode getPackage(String packageName) {
		SortedMutableTreeNode node = packageToNode.get(packageName);

		if (packageName == null) {
			return root;
		}

		if (node == null) {
			node = new ClassSelectorPackageNode(this.comparator, packageName);
			getPackage(ClassEntry.getParentPackage(packageName)).insert(node, 0);
			packageToNode.put(packageName, node);
		}

		return node;
	}

	public SortedMutableTreeNode getRoot() {
		return root;
	}

	public TreePath getPackagePath(String packageName) {
		SortedMutableTreeNode node = packageToNode.getOrDefault(packageName, root);
		return new TreePath(node.getPath());
	}

	public ClassSelectorClassNode getClassNode(ClassEntry entry) {
		return classToNode.get(entry);
	}

	public void removeClassNode(ClassEntry entry) {
		ClassSelectorClassNode node = classToNode.remove(entry);

		if (node != null) {
			node.removeFromParent();
			// remove dangling packages
			SortedMutableTreeNode packageNode = packageToNode.get(entry.getPackageName());

			while (packageNode != null && packageNode.getChildCount() == 0) {
				SortedMutableTreeNode theNode = packageNode;
				packageNode = (SortedMutableTreeNode) packageNode.getParent();
				theNode.removeFromParent();

				if (theNode instanceof ClassSelectorPackageNode pn) {
					packageToNode.remove(pn.getPackageName());
				}
			}
		}
	}

	public Collection<SortedMutableTreeNode> getPackageNodes() {
		return packageToNode.values();
	}
}
