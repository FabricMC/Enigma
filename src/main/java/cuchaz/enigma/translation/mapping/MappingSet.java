package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;

public interface MappingSet<M> {
	void insert(Entry entry, M mapping);

	void remove(Entry entry);

	@Nullable
	M getMapping(Entry entry);

	default boolean hasMapping(Entry entry) {
		return getMapping(entry) != null;
	}

	Collection<Entry> getEntries();
}
