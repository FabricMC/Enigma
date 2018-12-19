package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.MappingTree;

import java.io.IOException;
import java.nio.file.Path;

public interface MappingsWriter {
	void write(MappingTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path) throws IOException;
}
