package cuchaz.enigma.translation.mapping;

import com.google.common.collect.Sets;
import cuchaz.enigma.analysis.IndexTreeBuilder;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.index.BridgeMethodIndex;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.VoidTranslator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.*;

// TODO: This only resolves if not contained in the current class. Do we want that behaviour everywhere?
public class IndexEntryResolver implements EntryResolver {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;

	private final IndexTreeBuilder treeBuilder;

	public IndexEntryResolver(JarIndex index) {
		this.entryIndex = index.getEntryIndex();
		this.inheritanceIndex = index.getInheritanceIndex();
		this.bridgeMethodIndex = index.getBridgeMethodIndex();

		this.treeBuilder = new IndexTreeBuilder(index);
	}

	// TODO: bridge methods!
	@Override
	@SuppressWarnings("unchecked")
	public <E extends Entry<?>> E resolveEntry(E entry) {
		if (entry instanceof ClassEntry) {
			return entry;
		}

		List<Entry<?>> ancestry = entry.getAncestry();
		for (int i = ancestry.size() - 1; i > 0; i--) {
			Entry<?> child = ancestry.get(i);
			Entry<ClassEntry> cast = child.castParent(ClassEntry.class);
			if (cast != null && !(cast instanceof ClassEntry)) {
				// we found the entry which is a child of a class, we are now able to resolve the owner of this entry
				ClassEntry resolvedOwner = resolveEntryOwner(cast);
				if (resolvedOwner != null) {
					return (E) entry.replaceAncestor(cast.getParent(), resolvedOwner);
				}
			}
		}

		return entry;
	}

	@Override
	public <E extends Entry<ClassEntry>> ClassEntry resolveEntryOwner(E entry) {
		AccessFlags access = entryIndex.getEntryAccess(entry);

		// If we're private or static, it's impossible that this could be inherited from a parent class
		if (access != null && (access.isPrivate() || access.isStatic())) {
			return entry.getParent();
		}

		ClassEntry resolvedOwner = entry.getParent();

		LinkedList<ClassEntry> queue = new LinkedList<>();
		queue.add(entry.getParent());

		while (!queue.isEmpty()) {
			ClassEntry parentClass = queue.poll();

			Entry<ClassEntry> inheritedEntry = entry.withParent(parentClass);
			if (entryIndex.hasEntry(inheritedEntry)) {
				// We want to find the last parent class to contain this entry
				resolvedOwner = parentClass;
			}

			queue.addAll(inheritanceIndex.getParents(parentClass));
		}

		return resolvedOwner;
	}

	@Override
	public Collection<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		MethodEntry relevantMethod = entry.findAncestor(MethodEntry.class);
		if (relevantMethod == null || !entryIndex.hasMethod(relevantMethod)) {
			return Collections.singletonList(entry);
		}

		Collection<MethodEntry> equivalentMethods = resolveEquivalentMethods(relevantMethod);
		Collection<Entry<?>> equivalentEntries = new HashSet<>(equivalentMethods.size());

		for (MethodEntry equivalentMethod : equivalentMethods) {
			Entry<?> equivalentEntry = entry.replaceAncestor(relevantMethod, equivalentMethod);
			equivalentEntries.add(equivalentEntry);
		}

		return equivalentEntries;
	}

	@Override
	public Collection<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		AccessFlags access = entryIndex.getMethodAccess(methodEntry);
		if (access == null) {
			throw new IllegalArgumentException("Could not find method " + methodEntry);
		}

		if (!canInherit(methodEntry, access)) {
			return Collections.singleton(methodEntry);
		}

		Set<MethodEntry> methodEntries = Sets.newHashSet();
		resolveEquivalentMethods(methodEntries, treeBuilder.buildMethodInheritance(VoidTranslator.INSTANCE, methodEntry));
		return methodEntries;
	}

	private void resolveEquivalentMethods(Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (methodEntries.contains(methodEntry)) {
			return;
		}

		AccessFlags flags = entryIndex.getMethodAccess(methodEntry);
		if (flags != null && canInherit(methodEntry, flags)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = bridgeMethodIndex.getBridgedMethod(methodEntry);
		while (bridgedMethod != null) {
			methodEntries.addAll(resolveEquivalentMethods(bridgedMethod));
			bridgedMethod = bridgeMethodIndex.getBridgedMethod(bridgedMethod);
		}

		// look at interface methods too
		for (MethodImplementationsTreeNode implementationsNode : treeBuilder.buildMethodImplementations(VoidTranslator.INSTANCE, methodEntry)) {
			resolveEquivalentMethods(methodEntries, implementationsNode);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			resolveEquivalentMethods(methodEntries, (MethodInheritanceTreeNode) node.getChildAt(i));
		}
	}

	private void resolveEquivalentMethods(Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		AccessFlags flags = entryIndex.getMethodAccess(methodEntry);
		if (flags != null && !flags.isPrivate() && !flags.isStatic()) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = bridgeMethodIndex.getBridgedMethod(methodEntry);
		while (bridgedMethod != null) {
			methodEntries.addAll(resolveEquivalentMethods(bridgedMethod));
			bridgedMethod = bridgeMethodIndex.getBridgedMethod(bridgedMethod);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			resolveEquivalentMethods(methodEntries, (MethodImplementationsTreeNode) node.getChildAt(i));
		}
	}

	private boolean canInherit(MethodEntry entry, AccessFlags access) {
		return !entry.isConstructor() && !access.isPrivate() && !access.isStatic() && !access.isFinal();
	}
}
