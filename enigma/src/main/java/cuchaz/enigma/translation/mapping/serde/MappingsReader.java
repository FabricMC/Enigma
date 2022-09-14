package cuchaz.enigma.translation.mapping.serde;

import java.io.IOException;
import java.nio.file.Path;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

public interface MappingsReader {
	EntryTree<EntryMapping> read(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws MappingParseException, IOException;
}
