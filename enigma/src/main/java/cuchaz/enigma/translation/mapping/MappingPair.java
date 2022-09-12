package cuchaz.enigma.translation.mapping;

import javax.annotation.Nullable;

import cuchaz.enigma.translation.representation.entry.Entry;

public class MappingPair<E extends Entry<?>, M> {
	private final E entry;
	private M mapping;

	public MappingPair(E entry, @Nullable M mapping) {
		this.entry = entry;
		this.mapping = mapping;
	}

	public MappingPair(E entry) {
		this(entry, null);
	}

	public E getEntry() {
		return entry;
	}

	@Nullable
	public M getMapping() {
		return mapping;
	}

	public void setMapping(M mapping) {
		this.mapping = mapping;
	}
}
