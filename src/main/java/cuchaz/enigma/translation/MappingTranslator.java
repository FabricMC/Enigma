package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingSet;

public class MappingTranslator implements Translator {
	private final MappingSet<EntryMapping> mappings;

	public MappingTranslator(MappingSet<EntryMapping> mappings) {
		this.mappings = mappings;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Translatable> T translate(T translatable) {
		if (translatable == null) {
			return null;
		}
		return (T) translatable.translate(this, mappings);
	}
}
