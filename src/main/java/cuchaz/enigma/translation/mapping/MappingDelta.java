package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.HashMappingTree;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.Entry;

public class MappingDelta implements Translatable {
	public static final Object PLACEHOLDER = new Object();

	private final MappingTree<Object> additions;
	private final MappingTree<Object> deletions;

	public MappingDelta(MappingTree<Object> additions, MappingTree<Object> deletions) {
		this.additions = additions;
		this.deletions = deletions;
	}

	public MappingDelta() {
		this(new HashMappingTree<>(), new HashMappingTree<>());
	}

	public static MappingDelta added(MappingTree<EntryMapping> mappings) {
		MappingTree<Object> additions = new HashMappingTree<>();
		for (Entry<?> entry : mappings.getAllEntries()) {
			additions.insert(entry, PLACEHOLDER);
		}

		return new MappingDelta(additions, new HashMappingTree<>());
	}

	public MappingTree<?> getAdditions() {
		return additions;
	}

	public MappingTree<?> getDeletions() {
		return deletions;
	}

	@Override
	public MappingDelta translate(Translator translator, EntryResolver resolver, MappingSet<EntryMapping> mappings) {
		return new MappingDelta(
				translate(translator, additions),
				translate(translator, deletions)
		);
	}

	private MappingTree<Object> translate(Translator translator, MappingTree<Object> tree) {
		MappingTree<Object> translatedTree = new HashMappingTree<>();
		for (Entry<?> entry : tree.getAllEntries()) {
			translatedTree.insert(translator.translate(entry), PLACEHOLDER);
		}
		return translatedTree;
	}
}
