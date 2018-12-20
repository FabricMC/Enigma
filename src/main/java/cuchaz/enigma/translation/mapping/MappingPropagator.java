package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Consumer;

public class MappingPropagator {
	private final JarIndex jarIndex;
	private final MappingTree<EntryMapping> mappings;

	public MappingPropagator(JarIndex jarIndex, MappingTree<EntryMapping> mappings) {
		this.jarIndex = jarIndex;
		this.mappings = mappings;
	}

	public void propagateAction(Entry<?> entry, Consumer<Entry<?>> propagator) {
		getPropagationTargets(entry).forEach(propagator);
	}

	public Collection<Entry<?>> getPropagationTargets(Entry<?> entry) {
		MethodEntry relevantMethod = getRelevantMethod(entry);
		if (relevantMethod == null || !jarIndex.containsObfMethod(relevantMethod)) {
			return Collections.singletonList(entry);
		}

		Collection<Entry<?>> propagationTargets = new HashSet<>();
		propagationTargets.add(entry);

		Collection<MethodEntry> equivalentMethods = getEquivalentMethods(relevantMethod);
		for (MethodEntry equivalentMethod : equivalentMethods) {
			propagationTargets.add(equivalentMethod);

			Collection<Entry<?>> equivalentChildren = mappings.getChildren(equivalentMethod);
			for (Entry<?> equivalentChild : equivalentChildren) {
				if (equivalentChild.shallowEquals(entry)) {
					propagationTargets.add(equivalentChild);
				}
			}
		}

		return propagationTargets;
	}

	@Nullable
	private MethodEntry getRelevantMethod(Entry<?> entry) {
		return entry.findAncestor(MethodEntry.class);
	}

	private Collection<MethodEntry> getEquivalentMethods(MethodEntry method) {
		return jarIndex.getRelatedMethodImplementations(method);
	}
}
