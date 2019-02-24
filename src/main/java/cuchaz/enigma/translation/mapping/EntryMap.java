package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public interface EntryMap<T> {
	void insert(Entry<?> entry, T value);

	@Nullable
	T remove(Entry<?> entry);

	@Nullable
	T get(Entry<?> entry);

	default boolean contains(Entry<?> entry) {
		return get(entry) != null;
	}

	Stream<Entry<?>> getAllEntries();

	boolean isEmpty();
}
