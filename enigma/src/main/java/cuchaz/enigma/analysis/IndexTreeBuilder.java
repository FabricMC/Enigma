package cuchaz.enigma.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class IndexTreeBuilder {
	private final JarIndex index;

	public IndexTreeBuilder(JarIndex index) {
		this.index = index;
	}

	public ClassInheritanceTreeNode buildClassInheritance(Translator translator, ClassEntry obfClassEntry) {
		// get the root node
		List<String> ancestry = new ArrayList<>();
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
		MethodEntry resolvedEntry = index.getEntryResolver().resolveFirstEntry(obfMethodEntry, ResolutionStrategy.RESOLVE_ROOT);

		// make a root node at the base
		MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(translator, resolvedEntry, index.getEntryIndex().hasMethod(resolvedEntry));

		// expand the full tree
		rootNode.load(index);

		return rootNode;
	}

	public List<MethodImplementationsTreeNode> buildMethodImplementations(Translator translator, MethodEntry obfMethodEntry) {
		EntryResolver resolver = index.getEntryResolver();
		Collection<MethodEntry> resolvedEntries = resolver.resolveEntry(obfMethodEntry, ResolutionStrategy.RESOLVE_ROOT);

		List<MethodImplementationsTreeNode> nodes = new ArrayList<>();

		for (MethodEntry resolvedEntry : resolvedEntries) {
			MethodImplementationsTreeNode node = new MethodImplementationsTreeNode(translator, resolvedEntry);
			node.load(index);
			nodes.add(node);
		}

		return nodes;
	}
}
