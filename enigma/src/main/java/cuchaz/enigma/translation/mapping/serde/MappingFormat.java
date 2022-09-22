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
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE, null),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY, net.fabricmc.mappingio.format.MappingFormat.ENIGMA),
	ENIGMA_ZIP(EnigmaMappingsWriter.ZIP, EnigmaMappingsReader.ZIP, null),
	TINY_V2(new TinyV2Writer("intermediary", "named"), new TinyV2Reader(), net.fabricmc.mappingio.format.MappingFormat.TINY_2),
	TINY_FILE(TinyMappingsWriter.INSTANCE, TinyMappingsReader.INSTANCE, net.fabricmc.mappingio.format.MappingFormat.TINY),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null, net.fabricmc.mappingio.format.MappingFormat.SRG),
	PROGUARD(null, ProguardMappingsReader.INSTANCE, net.fabricmc.mappingio.format.MappingFormat.PROGUARD),
	RECAF(RecafMappingsWriter.INSTANCE, RecafMappingsReader.INSTANCE, null);

	private final MappingsWriter writer;
	private final MappingsReader reader;
	private final net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart;

	MappingFormat(MappingsWriter writer, MappingsReader reader, net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart) {
		this.writer = writer;
		this.reader = reader;
		this.mappingIoCounterpart = mappingIoCounterpart;
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

	@Nullable
	public net.fabricmc.mappingio.format.MappingFormat getMappingIoCounterpart() {
		return mappingIoCounterpart;
	}
}
