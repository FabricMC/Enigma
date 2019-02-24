package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.io.IOException;
import java.nio.file.Path;

public interface MappingsReader {
	EntryTree<EntryMapping> read(Path path, ProgressListener progress) throws MappingParseException, IOException;
}
