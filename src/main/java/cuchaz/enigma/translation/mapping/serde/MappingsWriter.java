package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.nio.file.Path;

public interface MappingsWriter {
	void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress);

	default void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progress) {
		write(mappings, MappingDelta.added(mappings), path, progress);
	}
}
