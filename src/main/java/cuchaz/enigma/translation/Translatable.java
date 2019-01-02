package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.EntryMap;

public interface Translatable {
	Translatable translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);
}
