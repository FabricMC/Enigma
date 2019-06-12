package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public enum MappingFormat {
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY),
	TINY_V1_FILE(new TinyMappingsWriter("obf", "deobf"), TinyMappingsReader.INSTANCE),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null);

	private final MappingsWriter writer;
	private final MappingsReader reader;

	MappingFormat(MappingsWriter writer, MappingsReader reader) {
		this.writer = writer;
		this.reader = reader;
	}

	public void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progressListener)  {
		write(mappings, MappingDelta.added(mappings), path, progressListener);
	}

	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progressListener)  {
		if (writer == null) {
			throw new IllegalStateException(name() + " does not support writing");
		}
		writer.write(mappings, delta, path, progressListener);
	}

	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener) throws IOException, MappingParseException {
		if (reader == null) {
			throw new IllegalStateException(name() + " does not support reading");
		}
		return reader.read(path, progressListener);
	}

	@Nullable
	public MappingsWriter getWriter() {
		return writer;
	}

	@Nullable
	public MappingsReader getReader() {
		return reader;
	}
}
