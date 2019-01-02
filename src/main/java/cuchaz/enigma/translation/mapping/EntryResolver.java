package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.List;

public interface EntryResolver {
	<E extends Entry<?>> E resolveEntry(E entry);

	<E extends Entry<ClassEntry>> ClassEntry resolveEntryOwner(E entry);

	default <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> resolveReference(EntryReference<E, C> reference) {
		E entry = resolveEntry(reference.entry);
		C context = resolveEntry(reference.context);
		return new EntryReference<>(entry, context, reference);
	}

	List<Entry<?>> resolveEquivalentEntries(Entry<?> entry);

	List<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry);
}
