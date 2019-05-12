package cuchaz.enigma.api;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Optional;

public interface EntryNameProposer {
	Optional<String> proposeName(Entry<?> obfEntry, EntryRemapper remapper);
}
