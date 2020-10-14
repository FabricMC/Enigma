package cuchaz.enigma.translation;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;

public class MappingTranslator implements Translator {
	private final EntryMap<EntryMapping> mappings;
	private final EntryResolver resolver;

	public MappingTranslator(EntryMap<EntryMapping> mappings, EntryResolver resolver) {
		this.mappings = mappings;
		this.resolver = resolver;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Translatable> TranslateResult<T> extendedTranslate(T translatable) {
		if (translatable == null) {
			return null;
		}
		return (TranslateResult<T>) translatable.extendedTranslate(this, resolver, mappings);
	}

}
