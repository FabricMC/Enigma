package cuchaz.enigma.translation.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public interface EntryResolver {
	<E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy);

	default <E extends Entry<?>> E resolveFirstEntry(E entry, ResolutionStrategy strategy) {
		return resolveEntry(entry, strategy).stream().findFirst().orElse(entry);
	}

	default <E extends Entry<?>, C extends Entry<?>> Collection<EntryReference<E, C>> resolveReference(EntryReference<E, C> reference, ResolutionStrategy strategy) {
		Collection<E> entry = resolveEntry(reference.entry, strategy);

		if (reference.context != null) {
			Collection<C> context = resolveEntry(reference.context, strategy);
			List<EntryReference<E, C>> result = new ArrayList<>(entry.size());
			Iterator<E> entryIterator = entry.iterator();
			Iterator<C> contextIterator = context.iterator();

			while (entryIterator.hasNext() && contextIterator.hasNext()) {
				result.add(new EntryReference<>(entryIterator.next(), contextIterator.next(), reference));
			}

			return result;
		} else {
			return entry.stream().map(e -> new EntryReference<>(e, null, reference)).toList();
		}
	}

	default <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> resolveFirstReference(EntryReference<E, C> reference, ResolutionStrategy strategy) {
		E entry = resolveFirstEntry(reference.entry, strategy);
		C context = resolveFirstEntry(reference.context, strategy);
		return new EntryReference<>(entry, context, reference);
	}

	Set<Entry<?>> resolveEquivalentEntries(Entry<?> entry);

	Set<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry);
}
