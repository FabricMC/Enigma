package cuchaz.enigma.translation.mapping;

import com.google.common.collect.Streams;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public interface EntryResolver {
	<E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy);

	default <E extends Entry<?>> E resolveFirstEntry(E entry, ResolutionStrategy strategy) {
		return resolveEntry(entry, strategy).stream().findFirst().orElse(entry);
	}

	default <E extends Entry<?>, C extends Entry<?>> Collection<EntryReference<E, C>> resolveReference(EntryReference<E, C> reference, ResolutionStrategy strategy) {
		Collection<E> entry = resolveEntry(reference.entry, strategy);
		Collection<C> context = resolveEntry(reference.context, strategy);
		return Streams.zip(entry.stream(), context.stream(), (e, c) -> new EntryReference<>(e, c, reference))
				.collect(Collectors.toList());
	}

	default <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> resolveFirstReference(EntryReference<E, C> reference, ResolutionStrategy strategy) {
		E entry = resolveFirstEntry(reference.entry, strategy);
		C context = resolveFirstEntry(reference.context, strategy);
		return new EntryReference<>(entry, context, reference);
	}

	Set<Entry<?>> resolveEquivalentEntries(Entry<?> entry);

	Set<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry);
}
