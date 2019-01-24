package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

public class MappingDelta implements Translatable {
	public static final Object PLACEHOLDER = new Object();

	private final EntryTree<Object> additions;
	private final EntryTree<Object> deletions;

	public MappingDelta(EntryTree<Object> additions, EntryTree<Object> deletions) {
		this.additions = additions;
		this.deletions = deletions;
	}

	public MappingDelta() {
		this(new HashEntryTree<>(), new HashEntryTree<>());
	}

	public static MappingDelta added(EntryTree<EntryMapping> mappings) {
		EntryTree<Object> additions = new HashEntryTree<>();
		for (Entry<?> entry : mappings.getAllEntries()) {
			additions.insert(entry, PLACEHOLDER);
		}

		return new MappingDelta(additions, new HashEntryTree<>());
	}

	public EntryTree<?> getAdditions() {
		return additions;
	}

	public EntryTree<?> getDeletions() {
		return deletions;
	}

	@Override
	public MappingDelta translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return new MappingDelta(
				translate(translator, additions),
				translate(translator, deletions)
		);
	}

	private EntryTree<Object> translate(Translator translator, EntryTree<Object> tree) {
		EntryTree<Object> translatedTree = new HashEntryTree<>();
		for (Entry<?> entry : tree.getAllEntries()) {
			translatedTree.insert(translator.translate(entry), PLACEHOLDER);
		}
		return translatedTree;
	}
}
