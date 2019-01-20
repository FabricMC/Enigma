package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum VoidEntryResolver implements EntryResolver {
	INSTANCE;

	@Override
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
		return Collections.singleton(entry);
	}

	@Override
	public List<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		return Collections.singletonList(entry);
	}

	@Override
	public List<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		return Collections.singletonList(methodEntry);
	}
}
