package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collections;
import java.util.List;

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
	public List<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		return Collections.singletonList(entry);
	}

	@Override
	public List<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		return Collections.singletonList(methodEntry);
	}
}
