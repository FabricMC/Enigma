package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.Translator;

public interface RemappableIndex {
	void remap(Translator translator);

	RemappableIndex remapped(Translator translator);
}
