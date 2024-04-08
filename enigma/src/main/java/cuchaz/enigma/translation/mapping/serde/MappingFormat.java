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
import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.MappingOperations;
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
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE, FileType.MAPPING, net.fabricmc.mappingio.format.MappingFormat.ENIGMA_FILE, true),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY, FileType.DIRECTORY, net.fabricmc.mappingio.format.MappingFormat.ENIGMA_DIR, true),
	ENIGMA_ZIP(EnigmaMappingsWriter.ZIP, EnigmaMappingsReader.ZIP, FileType.ZIP, null, false),
	TINY_V2(new TinyV2Writer("intermediary", "named"), new TinyV2Reader(), FileType.TINY, net.fabricmc.mappingio.format.MappingFormat.TINY_2_FILE, true),
	TINY_FILE(TinyMappingsWriter.INSTANCE, TinyMappingsReader.INSTANCE, FileType.TINY, net.fabricmc.mappingio.format.MappingFormat.TINY_FILE, true),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null, FileType.SRG, net.fabricmc.mappingio.format.MappingFormat.SRG_FILE, true),
	XSRG_FILE(null, null, FileType.XSRG, net.fabricmc.mappingio.format.MappingFormat.XSRG_FILE, true),
	CSRG_FILE(null, null, FileType.CSRG, net.fabricmc.mappingio.format.MappingFormat.CSRG_FILE, false),
	TSRG_FILE(null, null, FileType.TSRG, net.fabricmc.mappingio.format.MappingFormat.TSRG_FILE, false),
	TSRG_2_FILE(null, null, FileType.TSRG, net.fabricmc.mappingio.format.MappingFormat.TSRG_2_FILE, false),
	PROGUARD(null, ProguardMappingsReader.INSTANCE, FileType.TXT, net.fabricmc.mappingio.format.MappingFormat.PROGUARD_FILE, true),
	RECAF(RecafMappingsWriter.INSTANCE, RecafMappingsReader.INSTANCE, FileType.TXT, null, false);

	private final MappingsWriter writer;
	private final MappingsReader reader;
	private final FileType fileType;
	private final net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart;
	private final boolean hasMappingIoWriter;
	private boolean lastUsedMappingIoWriter;

	MappingFormat(MappingsWriter writer, MappingsReader reader, FileType fileType, net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart, boolean hasMappingIoWriter) {
		this.writer = writer;
		this.reader = reader;
		this.fileType = fileType;
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

			writer.write(mappings, lastUsedMappingIoWriter ? MappingDelta.added(mappings) : delta, path, progressListener, saveParameters);
			lastUsedMappingIoWriter = false;
			return;
		}

		try {
			if (this == PROGUARD) {
				mappings = MappingOperations.invert(mappings);
			}

			VisitableMappingTree tree = MappingIoConverter.toMappingIo(mappings, progressListener);
			progressListener.init(1, I18n.translate("progress.mappings.writing"));
			progressListener.step(1, null); // Reset message

			tree.accept(MappingWriter.create(path, mappingIoCounterpart), VisitOrder.createByName());
			progressListener.step(1, I18n.translate("progress.done"));
			lastUsedMappingIoWriter = true;
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
		EntryTree<EntryMapping> mappings = MappingIoConverter.fromMappingIo(mappingTree, progressListener, index);

		return this == PROGUARD ? MappingOperations.invert(mappings) : mappings;
	}

	/**
	 * @return Enigma's native writer for the format, or {@code null} if none exists.
	 *
	 * @deprecated Use {@link #isWritable()} and {@link #write(EntryTree, Path, ProgressListener, MappingSaveParameters)} instead,
	 * which take the new Mapping-IO equivalents (and eventual replacements) into account.
	 */
	@Nullable
	@Deprecated
	public MappingsWriter getWriter() {
		return writer;
	}

	/**
	 * @return Enigma's native reader for the format, or {@code null} if none exists.
	 *
	 * @deprecated Use {@link #isReadable()} and {@link #read(Path, ProgressListener, MappingSaveParameters, JarIndex)} instead,
	 * which take the new Mapping-IO equivalents (and eventual replacements) into account.
	 */
	@Nullable
	@Deprecated
	public MappingsReader getReader() {
		return reader;
	}

	@ApiStatus.Internal
	public FileType getFileType() {
		return fileType;
	}

	@Nullable
	@ApiStatus.Internal
	public net.fabricmc.mappingio.format.MappingFormat getMappingIoCounterpart() {
		return mappingIoCounterpart;
	}

	@ApiStatus.Internal
	public boolean hasMappingIoWriter() {
		return hasMappingIoWriter;
	}

	public boolean isReadable() {
		return reader != null || mappingIoCounterpart != null;
	}

	public boolean isWritable() {
		return writer != null || hasMappingIoWriter;
	}

	@ApiStatus.Internal
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

	/**
	 * A file type. It can be either a single file with an extension, or a directory
	 * with a {@code null} extension.
	 *
	 * <p>If a file type has multiple extensions, the default for saving will be the first one.
	 *
	 * @param extensions the file extensions with the leading dot {@code .}, or an empty list for a directory
	 */
	@ApiStatus.Internal
	public record FileType(List<String> extensions) {
		public static final FileType DIRECTORY = new FileType();
		public static final FileType MAPPING = new FileType(".mapping", ".mappings");
		public static final FileType SRG = new FileType(".srg");
		public static final FileType XSRG = new FileType(".xsrg");
		public static final FileType CSRG = new FileType(".csrg");
		public static final FileType TSRG = new FileType(".tsrg");
		public static final FileType TINY = new FileType(".tiny");
		public static final FileType TXT = new FileType(".txt");
		public static final FileType ZIP = new FileType(".zip");

		public FileType(String... extensions) {
			this(List.of(extensions));
		}

		public boolean isDirectory() {
			return extensions.isEmpty();
		}
	}
}
