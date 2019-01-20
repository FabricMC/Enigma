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
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry) {
		if (entry == null || entry instanceof ClassEntry) {
			return Collections.singleton(entry);
		}

		Entry<ClassEntry> classChild = getClassChild(entry);
		if (classChild != null && !(classChild instanceof ClassEntry)) {
			// we found the entry which is a child of a class, we are now able to resolve the owner of this entry
			Collection<ClassEntry> resolvedOwners = resolveEntryOwners(classChild);
			return resolvedOwners.stream()
					.map(owner -> (E) entry.replaceAncestor(classChild.getParent(), owner))
					.collect(Collectors.toList());
		}

		return Collections.singleton(entry);
	}

	@Nullable
	private Entry<ClassEntry> getClassChild(Entry<?> entry) {
		// get the entry in the heirarchy that is the child of a class
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

	// TODO: bridges
	@Override
	public <E extends Entry<ClassEntry>> Collection<ClassEntry> resolveEntryOwners(E entry) {
		AccessFlags access = entryIndex.getEntryAccess(entry);

		// If we're private, it's impossible that this could be inherited from a parent class
		if (access != null && access.isPrivate()) {
			return Collections.singleton(entry.getParent());
		}

		Set<ClassEntry> owners = resolveParentOwners(entry);
		if (owners.isEmpty()) {
			owners.add(entry.getParent());
		}

		return owners;
	}

	private Set<ClassEntry> resolveParentOwners(Entry<ClassEntry> entry) {
		ClassEntry ownerClass = entry.getParent();

		Set<ClassEntry> owners = new HashSet<>();
		for (ClassEntry parentClass : inheritanceIndex.getParents(ownerClass)) {
			Entry<ClassEntry> parentEntry = entry.withParent(parentClass);
			Set<ClassEntry> parentOwners = resolveParentOwners(parentEntry);

			if (!parentOwners.isEmpty()) {
				owners.addAll(parentOwners);
			} else {
				AccessFlags parentAccess = entryIndex.getEntryAccess(parentEntry);
				if (parentAccess != null && !parentAccess.isPrivate()) {
					owners.add(parentClass);
				}
			}
		}

		return owners;
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
