package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingSet;

public interface Translatable {
	Translatable translate(Translator translator, MappingSet<EntryMapping> mappings);
}
