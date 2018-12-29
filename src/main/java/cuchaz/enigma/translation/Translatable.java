package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.MappingSet;

public interface Translatable {
	Translatable translate(Translator translator, EntryResolver resolver, MappingSet<EntryMapping> mappings);
}
