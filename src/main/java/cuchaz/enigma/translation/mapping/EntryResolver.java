package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;

public interface EntryResolver {
	<E extends Entry<?>> E resolveEntry(E entry);

	<E extends Entry<ClassEntry>> ClassEntry resolveEntryOwner(E entry);

	Collection<Entry<?>> resolveEquivalentEntries(Entry<?> entry);

	Collection<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry);
}
