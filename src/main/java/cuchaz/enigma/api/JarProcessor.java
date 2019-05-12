package cuchaz.enigma.api;

import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.JarIndex;

public interface JarProcessor {
	void accept(ParsedJar jar, JarIndex index);
}
