package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface RemappableIndex {
	RemappableIndex remapped(Translator translator);

	void remapEntry(Entry<?> entry, Entry<?> newEntry);
}
