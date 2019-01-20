package cuchaz.enigma.translation.mapping;

import com.google.common.collect.Streams;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.*;
import java.util.stream.Collectors;

public interface EntryResolver {
	<E extends Entry<?>> Collection<E> resolveEntry(E entry);

	<E extends Entry<ClassEntry>> Collection<ClassEntry> resolveEntryOwners(E entry);

	default <E extends Entry<?>, C extends Entry<?>> Collection<EntryReference<E, C>> resolveReference(EntryReference<E, C> reference) {
		Collection<E> entry = resolveEntry(reference.entry);
		Collection<C> context = resolveEntry(reference.context);
		return Streams.zip(entry.stream(), context.stream(), (e, c) -> new EntryReference<>(e, c, reference))
				.collect(Collectors.toList());
	}

	List<Entry<?>> resolveEquivalentEntries(Entry<?> entry);

	List<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry);
}
