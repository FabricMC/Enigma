package cuchaz.enigma.translation.mapping.serde;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitOrder;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
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
import cuchaz.enigma.utils.I18n;

public enum MappingFormat {
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE, net.fabricmc.mappingio.format.MappingFormat.ENIGMA_FILE, true),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY, net.fabricmc.mappingio.format.MappingFormat.ENIGMA_DIR, true),
	ENIGMA_ZIP(EnigmaMappingsWriter.ZIP, EnigmaMappingsReader.ZIP, null, false),
	TINY_V2(new TinyV2Writer("intermediary", "named"), new TinyV2Reader(), net.fabricmc.mappingio.format.MappingFormat.TINY_2_FILE, true),
	TINY_FILE(TinyMappingsWriter.INSTANCE, TinyMappingsReader.INSTANCE, net.fabricmc.mappingio.format.MappingFormat.TINY_FILE, true),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null, net.fabricmc.mappingio.format.MappingFormat.SRG_FILE, true),
	XSRG_FILE(null, null, net.fabricmc.mappingio.format.MappingFormat.XSRG_FILE, true),
	CSRG_FILE(null, null, net.fabricmc.mappingio.format.MappingFormat.CSRG_FILE, false),
	TSRG_FILE(null, null, net.fabricmc.mappingio.format.MappingFormat.TSRG_FILE, false),
	TSRG_2_FILE(null, null, net.fabricmc.mappingio.format.MappingFormat.TSRG_2_FILE, false),
	PROGUARD(null, ProguardMappingsReader.INSTANCE, net.fabricmc.mappingio.format.MappingFormat.PROGUARD_FILE, true),
	RECAF(RecafMappingsWriter.INSTANCE, RecafMappingsReader.INSTANCE, null, false);

	private final MappingsWriter writer;
	private final MappingsReader reader;
	private final net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart;
	private final boolean hasMappingIoWriter;

	MappingFormat(MappingsWriter writer, MappingsReader reader, net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart, boolean hasMappingIoWriter) {
		this.writer = writer;
		this.reader = reader;
		this.mappingIoCounterpart = mappingIoCounterpart;
		this.hasMappingIoWriter = hasMappingIoWriter;
	}

	public void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		write(mappings, MappingDelta.added(mappings), path, progressListener, saveParameters);
	}

	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		if (!hasMappingIoWriter || !useMappingIo()) {
			if (writer == null) {
				throw new IllegalStateException(name() + " does not support writing");
			}

			writer.write(mappings, delta, path, progressListener, saveParameters);
			return;
		}

		try {
			VisitableMappingTree tree = MappingIoConverter.toMappingIo(mappings, progressListener);
			progressListener.init(1, I18n.translate("progress.mappings.writing"));
			progressListener.step(1, null); // Reset message

			tree.accept(MappingWriter.create(path, mappingIoCounterpart), VisitOrder.createByName());
			progressListener.step(1, I18n.translate("progress.done"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Deprecated
	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
		return read(path, progressListener, saveParameters, null);
	}

	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener, MappingSaveParameters saveParameters, JarIndex index) throws IOException, MappingParseException {
		if (!useMappingIo()) {
			if (reader == null) {
				throw new IllegalStateException(name() + " does not support reading");
			}

			return reader.read(path, progressListener, saveParameters);
		}

		String loadingMessage;

		if (mappingIoCounterpart.hasSingleFile()) {
			loadingMessage = I18n.translate("progress.mappings.loading_file");
		} else {
			loadingMessage = I18n.translate("progress.mappings.loading_directory");
		}

		progressListener.init(1, loadingMessage);

		VisitableMappingTree mappingTree = new MemoryMappingTree();
		MappingReader.read(path, mappingIoCounterpart, mappingTree);
		return MappingIoConverter.fromMappingIo(mappingTree, progressListener, index);
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

	public boolean hasMappingIoWriter() {
		return hasMappingIoWriter;
	}

	public boolean isReadable() {
		return reader != null || mappingIoCounterpart != null;
	}

	public boolean isWritable() {
		return writer != null || hasMappingIoWriter;
	}

	private boolean useMappingIo() {
		if (mappingIoCounterpart == null) return false;
		return System.getProperty("enigma.use_mappingio", "true").equals("true");
	}

	public static List<MappingFormat> getReadableFormats() {
		return Arrays.asList(values())
				.stream()
				.filter(MappingFormat::isReadable)
				.toList();
	}

	public static List<MappingFormat> getWritableFormats() {
		return Arrays.asList(values())
				.stream()
				.filter(MappingFormat::isWritable)
				.toList();
	}
}
