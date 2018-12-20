package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.HashMappingTree;
import cuchaz.enigma.translation.mapping.tree.MappingTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public enum MappingFormat {
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY),
	TINY_FILE(null, TinyMappingsReader.INSTANCE),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null);

	private final MappingsWriter writer;
	private final MappingsReader reader;

	MappingFormat(MappingsWriter writer, MappingsReader reader) {
		this.writer = writer;
		this.reader = reader;
	}

	public void write(MappingTree<EntryMapping> mappings, Path path) throws IOException {
		write(mappings, new MappingDelta<>(mappings, new HashMappingTree<>()), path);
	}

	public void write(MappingTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path) throws IOException {
		if (writer == null) {
			throw new IllegalStateException(name() + " does not support writing");
		}
		writer.write(mappings, delta, path);
	}

	public MappingTree<EntryMapping> read(Path path) throws IOException, MappingParseException {
		if (reader == null) {
			throw new IllegalStateException(name() + " does not support reading");
		}
		return reader.read(path);
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