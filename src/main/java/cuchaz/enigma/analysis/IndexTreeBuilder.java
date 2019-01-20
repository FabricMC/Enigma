package cuchaz.enigma.analysis;

import com.google.common.collect.Lists;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.List;

public class IndexTreeBuilder {
	private final JarIndex index;

	public IndexTreeBuilder(JarIndex index) {
		this.index = index;
	}

	public ClassInheritanceTreeNode buildClassInheritance(Translator translator, ClassEntry obfClassEntry) {
		// get the root node
		List<String> ancestry = Lists.newArrayList();
		ancestry.add(obfClassEntry.getFullName());
		for (ClassEntry classEntry : index.getInheritanceIndex().getAncestors(obfClassEntry)) {
			ancestry.add(classEntry.getFullName());
		}

		ClassInheritanceTreeNode rootNode = new ClassInheritanceTreeNode(translator, ancestry.get(ancestry.size() - 1));

		// expand all children recursively
		rootNode.load(index.getInheritanceIndex(), true);

		return rootNode;
	}

	public ClassImplementationsTreeNode buildClassImplementations(Translator translator, ClassEntry obfClassEntry) {
		if (index.getInheritanceIndex().isParent(obfClassEntry)) {
			ClassImplementationsTreeNode node = new ClassImplementationsTreeNode(translator, obfClassEntry);
			node.load(index);
			return node;
		}
		return null;
	}

	public MethodInheritanceTreeNode buildMethodInheritance(Translator translator, MethodEntry obfMethodEntry) {
		Collection<MethodEntry> resolvedEntries = index.getEntryResolver().resolveEntry(obfMethodEntry);
		MethodEntry resolvedEntry = resolvedEntries.stream().findAny().orElse(obfMethodEntry);

		// make a root node at the base
		MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(
				translator, resolvedEntry,
				index.getEntryIndex().hasMethod(resolvedEntry)
		);

		// expand the full tree
		rootNode.load(index, true);

		return rootNode;
	}

	public List<MethodImplementationsTreeNode> buildMethodImplementations(Translator translator, MethodEntry obfMethodEntry) {
		EntryIndex entryIndex = index.getEntryIndex();

		List<MethodEntry> ancestorMethodEntries = Lists.newArrayList();

		if (entryIndex.hasMethod(obfMethodEntry)) {
			ancestorMethodEntries.add(obfMethodEntry);
		}

		for (ClassEntry ancestorEntry : index.getInheritanceIndex().getAncestors(obfMethodEntry.getParent())) {
			MethodEntry ancestorMethod = obfMethodEntry.withParent(ancestorEntry);
			if (entryIndex.hasMethod(ancestorMethod)) {
				ancestorMethodEntries.add(ancestorMethod);
			}
		}

		List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
		if (!ancestorMethodEntries.isEmpty()) {
			for (MethodEntry interfaceMethodEntry : ancestorMethodEntries) {
				MethodImplementationsTreeNode node = new MethodImplementationsTreeNode(translator, interfaceMethodEntry);
				node.load(index);
				nodes.add(node);
			}
		}

		return nodes;
	}
}
