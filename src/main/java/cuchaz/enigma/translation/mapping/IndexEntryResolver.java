package cuchaz.enigma.translation.mapping;

import com.google.common.collect.Lists;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

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

	@Override
	@SuppressWarnings("unchecked")
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
		if (entry == null) {
			return Collections.emptySet();
		}

		Entry<ClassEntry> classChild = getClassChild(entry);
		if (classChild != null && !(classChild instanceof ClassEntry)) {
			AccessFlags access = entryIndex.getEntryAccess(classChild);

			// TODO: bridges
			if (canBeInherited(access)) {
				Collection<Entry<ClassEntry>> resolvedChildren = resolveChildEntry(classChild, strategy);
				if (!resolvedChildren.isEmpty()) {
					return resolvedChildren.stream()
							.map(resolvedChild -> (E) entry.replaceAncestor(classChild, resolvedChild))
							.collect(Collectors.toList());
				}
			}
		}

		return Collections.singleton(entry);
	}

	@Nullable
	private Entry<ClassEntry> getClassChild(Entry<?> entry) {
		if (entry instanceof ClassEntry) {
			return null;
		}

		// get the entry in the hierarchy that is the child of a class
		List<Entry<?>> ancestry = entry.getAncestry();
		for (int i = ancestry.size() - 1; i > 0; i--) {
			Entry<?> child = ancestry.get(i);
			Entry<ClassEntry> cast = child.castParent(ClassEntry.class);
			if (cast != null && !(cast instanceof ClassEntry)) {
				// we found the entry which is a child of a class, we are now able to resolve the owner of this entry
				return cast;
			}
		}

		return null;
	}

	private boolean canBeInherited(AccessFlags access) {
		return access == null || !access.isPrivate();
	}

	private Set<Entry<ClassEntry>> resolveChildEntry(Entry<ClassEntry> entry, ResolutionStrategy strategy) {
		ClassEntry ownerClass = entry.getParent();

		Set<Entry<ClassEntry>> resolvedEntries = new HashSet<>();

		for (ClassEntry parentClass : inheritanceIndex.getParents(ownerClass)) {
			Entry<ClassEntry> parentEntry = entry.withParent(parentClass);

			if (strategy == ResolutionStrategy.RESOLVE_ROOT) {
				resolvedEntries.addAll(resolveRoot(parentEntry, strategy));
			} else {
				resolvedEntries.addAll(resolveClosest(parentEntry, strategy));
			}
		}

		return resolvedEntries;
	}

	private Collection<Entry<ClassEntry>> resolveRoot(Entry<ClassEntry> entry, ResolutionStrategy strategy) {
		// When resolving root, we want to first look for the lowest entry before returning ourselves
		Set<Entry<ClassEntry>> parentResolution = resolveChildEntry(entry, strategy);

		if (parentResolution.isEmpty()) {
			AccessFlags parentAccess = entryIndex.getEntryAccess(entry);
			if (parentAccess != null && !parentAccess.isPrivate()) {
				return Collections.singleton(entry);
			}
		}

		return parentResolution;
	}

	private Collection<Entry<ClassEntry>> resolveClosest(Entry<ClassEntry> entry, ResolutionStrategy strategy) {
		// When resolving closest, we want to first check if we exist before looking further down
		AccessFlags parentAccess = entryIndex.getEntryAccess(entry);
		if (parentAccess != null && !parentAccess.isPrivate()) {
			return Collections.singleton(entry);
		} else {
			return resolveChildEntry(entry, strategy);
		}
	}

	@Override
	public List<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		MethodEntry relevantMethod = entry.findAncestor(MethodEntry.class);
		if (relevantMethod == null || !entryIndex.hasMethod(relevantMethod)) {
			return Collections.singletonList(entry);
		}

		List<MethodEntry> equivalentMethods = resolveEquivalentMethods(relevantMethod);
		List<Entry<?>> equivalentEntries = new ArrayList<>(equivalentMethods.size());

		for (MethodEntry equivalentMethod : equivalentMethods) {
			Entry<?> equivalentEntry = entry.replaceAncestor(relevantMethod, equivalentMethod);
			equivalentEntries.add(equivalentEntry);
		}

		return equivalentEntries;
	}

	@Override
	public List<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		AccessFlags access = entryIndex.getMethodAccess(methodEntry);
		if (access == null) {
			throw new IllegalArgumentException("Could not find method " + methodEntry);
		}

		if (!canInherit(methodEntry, access)) {
			return Collections.singletonList(methodEntry);
		}

		List<MethodEntry> methodEntries = Lists.newArrayList();
		resolveEquivalentMethods(methodEntries, treeBuilder.buildMethodInheritance(VoidTranslator.INSTANCE, methodEntry));
		return methodEntries;
	}

	private void resolveEquivalentMethods(List<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (methodEntries.contains(methodEntry)) {
			return;
		}

		AccessFlags flags = entryIndex.getMethodAccess(methodEntry);
		if (flags != null && canInherit(methodEntry, flags)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// TODO: This might be the wrong direction
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

	private void resolveEquivalentMethods(List<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
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
