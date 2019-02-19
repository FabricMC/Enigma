package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

public class MappingDelta<T> implements Translatable {
	public static final Object PLACEHOLDER = new Object();

	private final EntryTree<T> baseMappings;

	private final EntryTree<Object> additions;
	private final EntryTree<Object> deletions;

	public MappingDelta(EntryTree<T> baseMappings, EntryTree<Object> additions, EntryTree<Object> deletions) {
		this.baseMappings = baseMappings;
		this.additions = additions;
		this.deletions = deletions;
	}

	public MappingDelta(EntryTree<T> baseMappings) {
		this(baseMappings, new HashEntryTree<>(), new HashEntryTree<>());
	}

	public static <T> MappingDelta<T> added(EntryTree<T> mappings) {
		EntryTree<Object> additions = new HashEntryTree<>();
		for (Entry<?> entry : mappings.getAllEntries()) {
			additions.insert(entry, PLACEHOLDER);
		}

		return new MappingDelta<>(new HashEntryTree<>(), additions, new HashEntryTree<>());
	}

	public EntryTree<T> getBaseMappings() {
		return baseMappings;
	}

	public EntryTree<?> getAdditions() {
		return additions;
	}

	public EntryTree<?> getDeletions() {
		return deletions;
	}

	@Override
	public MappingDelta<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return new MappingDelta<>(
				translator.translate(baseMappings),
				translator.translate(additions),
				translator.translate(deletions)
		);
	}
}
