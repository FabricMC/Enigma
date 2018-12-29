package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.Collections;

public enum VoidEntryResolver implements EntryResolver {
	INSTANCE;

	@Override
	public <E extends Entry<?>> E resolveEntry(E entry) {
		return entry;
	}

	@Override
	public <E extends Entry<ClassEntry>> ClassEntry resolveEntryOwner(E entry) {
		return entry.getParent();
	}

	@Override
	public Collection<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		return Collections.singleton(entry);
	}

	@Override
	public Collection<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		return Collections.singleton(methodEntry);
	}
}
