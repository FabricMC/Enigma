package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.mapping.tree.HashMappingTree;
import cuchaz.enigma.translation.mapping.tree.MappingTree;

public class MappingDelta<M> {
	private final MappingTree<M> additions;
	private final MappingTree<M> deletions;

	public MappingDelta(MappingTree<M> additions, MappingTree<M> deletions) {
		this.additions = additions;
		this.deletions = deletions;
	}

	public MappingDelta() {
		this(new HashMappingTree<>(), new HashMappingTree<>());
	}

	public MappingTree<M> getAdditions() {
		return additions;
	}

	public MappingTree<M> getDeletions() {
		return deletions;
	}
}
