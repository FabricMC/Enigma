package cuchaz.enigma.gui;

import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class NestedPackages {

	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private final Map<String, DefaultMutableTreeNode> packageToNode = new HashMap<>();
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

		for (var entry : entries) {
			addEntry(entry);
		}
	}

	public void addEntry(ClassEntry entry) {
		var translated = remapper.deobfuscate(entry);
		var me = new ClassSelectorClassNode(entry, translated);
		classToNode.put(entry, me);
		insert(getPackage(translated.getPackageName()), me);
	}

	public DefaultMutableTreeNode getPackage(String packageName) {
		var node = packageToNode.get(packageName);

		if (packageName == null) {
			return root;
		}

		if (node == null) {
			node = new ClassSelectorPackageNode(packageName);
			insert(getPackage(ClassEntry.getParentPackage(packageName)), node);
			packageToNode.put(packageName, node);
		}

		return node;
	}

	public DefaultMutableTreeNode getRoot() {
		return root;
	}

	public TreePath getPackagePath(String packageName) {
		var node = packageToNode.getOrDefault(packageName, root);
		return new TreePath(node.getPath());
	}

	public ClassSelectorClassNode getClassNode(ClassEntry entry) {
		return classToNode.get(entry);
	}

	public void removeClassNode(ClassEntry entry) {
		var node = classToNode.remove(entry);

		if (node != null) {
			node.removeFromParent();
			// remove dangling packages
			var packageNode = packageToNode.get(entry.getPackageName());

			while (packageNode != null && packageNode.getChildCount() == 0) {
				var theNode = packageNode;
				packageNode = (DefaultMutableTreeNode) packageNode.getParent();
				theNode.removeFromParent();

				if (theNode instanceof ClassSelectorPackageNode pn) {
					packageToNode.remove(pn.getPackageName());
				}
			}
		}
	}

	public Collection<DefaultMutableTreeNode> getPackageNodes() {
		return packageToNode.values();
	}

	private void insert(DefaultMutableTreeNode parent, MutableTreeNode child) {
		var index = 0;
		var children = parent.children();

		while (children.hasMoreElements()) {
			if (comparator.compare(children.nextElement(), child) < 0) {
				index++;
			} else {
				break;
			}
		}

		parent.insert(child, index);
	}
}
