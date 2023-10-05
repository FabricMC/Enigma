package cuchaz.enigma.translation.mapping.serde;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nullable;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.proguard.ProguardMappingsReader;
import cuchaz.enigma.translation.mapping.serde.recaf.RecafMappingsReader;
import cuchaz.enigma.translation.mapping.serde.recaf.RecafMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.srg.SrgMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.tiny.TinyMappingsReader;
import cuchaz.enigma.translation.mapping.serde.tiny.TinyMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Reader;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

public enum MappingFormat {
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE, FileType.MAPPING),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY, FileType.DIRECTORY),
	ENIGMA_ZIP(EnigmaMappingsWriter.ZIP, EnigmaMappingsReader.ZIP, FileType.ZIP),
	TINY_V2(new TinyV2Writer("intermediary", "named"), new TinyV2Reader(), FileType.TINY),
	TINY_FILE(TinyMappingsWriter.INSTANCE, TinyMappingsReader.INSTANCE, FileType.TINY),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null, FileType.SRG),
	PROGUARD(null, ProguardMappingsReader.INSTANCE, FileType.TXT),
	RECAF(RecafMappingsWriter.INSTANCE, RecafMappingsReader.INSTANCE, FileType.TXT);

	private final MappingsWriter writer;
	private final MappingsReader reader;
	private final FileType fileType;

	MappingFormat(MappingsWriter writer, MappingsReader reader, FileType fileType) {
		this.writer = writer;
		this.reader = reader;
		this.fileType = fileType;
	}

	public void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		write(mappings, MappingDelta.added(mappings), path, progressListener, saveParameters);
	}

	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		if (writer == null) {
			throw new IllegalStateException(name() + " does not support writing");
		}

		writer.write(mappings, delta, path, progressListener, saveParameters);
	}

	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
		if (reader == null) {
			throw new IllegalStateException(name() + " does not support reading");
		}

		return reader.read(path, progressListener, saveParameters);
	}

	@Nullable
	public MappingsWriter getWriter() {
		return writer;
	}

	@Nullable
	public MappingsReader getReader() {
		return reader;
	}

	public FileType getFileType() {
		return fileType;
	}

	/**
	 * A file type. It can be either a single file with an extension, or a directory
	 * with a {@code null} extension.
	 *
	 * @param extension the file extension with the leading dot {@code .}, or {@code null} for a directory
	 */
	public record FileType(@Nullable String extension) {
		public static final FileType DIRECTORY = new FileType(null);
		public static final FileType MAPPING = new FileType(".mapping");
		public static final FileType SRG = new FileType(".srg");
		public static final FileType TINY = new FileType(".tiny");
		public static final FileType TXT = new FileType(".txt");
		public static final FileType ZIP = new FileType(".zip");

		public boolean isDirectory() {
			return extension == null;
		}
	}
}
