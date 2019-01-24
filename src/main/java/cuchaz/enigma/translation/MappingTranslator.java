package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.EntryMap;

public class MappingTranslator implements Translator {
	private final EntryMap<EntryMapping> mappings;
	private final EntryResolver resolver;

	public MappingTranslator(EntryMap<EntryMapping> mappings, EntryResolver resolver) {
		this.mappings = mappings;
		this.resolver = resolver;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Translatable> T translate(T translatable) {
		if (translatable == null) {
			return null;
		}
		return (T) translatable.translate(this, resolver, mappings);
	}
}
