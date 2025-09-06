package cuchaz.enigma.translation.mapping.serde;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.utils.I18n;

public enum MappingFormat {
	ENIGMA_FILE(FileType.MAPPING, net.fabricmc.mappingio.format.MappingFormat.ENIGMA_FILE),
	ENIGMA_DIRECTORY(FileType.DIRECTORY, net.fabricmc.mappingio.format.MappingFormat.ENIGMA_DIR),
	TINY_V2(FileType.TINY, net.fabricmc.mappingio.format.MappingFormat.TINY_2_FILE),
	TINY_FILE(FileType.TINY, net.fabricmc.mappingio.format.MappingFormat.TINY_FILE),
	SRG_FILE(FileType.SRG, net.fabricmc.mappingio.format.MappingFormat.SRG_FILE),
	XSRG_FILE(FileType.XSRG, net.fabricmc.mappingio.format.MappingFormat.XSRG_FILE),
	JAM_FILE(FileType.JAM, net.fabricmc.mappingio.format.MappingFormat.JAM_FILE),
	CSRG_FILE(FileType.CSRG, net.fabricmc.mappingio.format.MappingFormat.CSRG_FILE),
	TSRG_FILE(FileType.TSRG, net.fabricmc.mappingio.format.MappingFormat.TSRG_FILE),
	TSRG_2_FILE(FileType.TSRG, net.fabricmc.mappingio.format.MappingFormat.TSRG_2_FILE),
	PROGUARD(FileType.TXT, net.fabricmc.mappingio.format.MappingFormat.PROGUARD_FILE),
	RECAF(FileType.TXT, net.fabricmc.mappingio.format.MappingFormat.RECAF_SIMPLE_FILE),
	JOBF_FILE(FileType.JOBF, net.fabricmc.mappingio.format.MappingFormat.JOBF_FILE),
	INTELLIJ_MIGRATION_MAP_FILE(FileType.XML, net.fabricmc.mappingio.format.MappingFormat.INTELLIJ_MIGRATION_MAP_FILE);

	private final FileType fileType;
	private final net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart;
	private final boolean hasWriter;
	private boolean usedMappingIoWriterLast;

	MappingFormat(FileType fileType, net.fabricmc.mappingio.format.MappingFormat mappingIoCounterpart) {
		this.fileType = fileType;
		this.mappingIoCounterpart = Objects.requireNonNull(mappingIoCounterpart);
		this.hasWriter = mappingIoCounterpart.hasWriter;
	}

	public void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		write(mappings, MappingDelta.added(mappings), path, progressListener, saveParameters);
	}

	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		if (!hasWriter) {
			throw new UnsupportedOperationException("Mapping format " + this + " does not support writing");
		}

		try {
			if (this == ENIGMA_DIRECTORY) { // TODO: Remove once MIO supports deltas
				EnigmaMappingsWriter.DIRECTORY.write(mappings, usedMappingIoWriterLast ? MappingDelta.added(mappings) : delta, path, progressListener, saveParameters, true);
				usedMappingIoWriterLast = false;
			} else {
				if (this == PROGUARD) {
					mappings = MappingOperations.invert(mappings);
				}

				VisitableMappingTree tree = MappingIoConverter.toMappingIo(mappings, progressListener);
				progressListener.init(1, I18n.translate("progress.mappings.writing"));
				progressListener.step(1, null); // Reset message

				tree.accept(MappingWriter.create(path, mappingIoCounterpart), VisitOrder.createByName());
				progressListener.step(1, I18n.translate("progress.done"));
				usedMappingIoWriterLast = true;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Deprecated
	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
		return read(path, progressListener, saveParameters, null);
	}

	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener, MappingSaveParameters saveParameters, JarIndex index) throws IOException, MappingParseException {
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

	@ApiStatus.Internal
	public FileType getFileType() {
		return fileType;
	}

	public boolean isReadable() {
		return true;
	}

	public boolean isWritable() {
		return hasWriter;
	}

	public static List<MappingFormat> getReadableFormats() {
		return Arrays.stream(values())
				.filter(MappingFormat::isReadable)
				.toList();
	}

	public static List<MappingFormat> getWritableFormats() {
		return Arrays.stream(values())
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
		public static final FileType TINY = new FileType(".tiny");
		public static final FileType SRG = new FileType(".srg");
		public static final FileType XSRG = new FileType(".xsrg");
		public static final FileType JAM = new FileType(".jam");
		public static final FileType CSRG = new FileType(".csrg");
		public static final FileType TSRG = new FileType(".tsrg");
		public static final FileType TXT = new FileType(".txt");
		public static final FileType JOBF = new FileType(".jobf");
		public static final FileType XML = new FileType(".xml");

		public FileType(String... extensions) {
			this(List.of(extensions));
		}

		public boolean isDirectory() {
			return extensions.isEmpty();
		}
	}
}
