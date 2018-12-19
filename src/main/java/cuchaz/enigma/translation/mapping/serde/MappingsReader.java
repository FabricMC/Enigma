package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.MappingTree;

import java.io.IOException;
import java.nio.file.Path;

public interface MappingsReader {
	MappingTree<EntryMapping> read(Path path) throws MappingParseException, IOException;
}
