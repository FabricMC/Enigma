package cuchaz.enigma.translation.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public enum VoidEntryResolver implements EntryResolver {
	INSTANCE;

	@Override
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
		return Collections.singleton(entry);
	}

	@Override
	public Set<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		return Collections.singleton(entry);
	}

	@Override
	public Set<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		return Collections.singleton(methodEntry);
	}
}
