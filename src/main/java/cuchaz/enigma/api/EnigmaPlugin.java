package cuchaz.enigma.api;

import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Optional;

public interface EnigmaPlugin {
	void indexJar(ParsedJar jar, JarIndex index);

	Optional<String> proposeName(Entry<?> obfEntry, EntryRemapper remapper);
}
