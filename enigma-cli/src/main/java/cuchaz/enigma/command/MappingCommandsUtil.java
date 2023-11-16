package cuchaz.enigma.command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingIoConverter;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.tiny.TinyMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

public final class MappingCommandsUtil {
	private MappingCommandsUtil() {
	}

	public static EntryTree<EntryMapping> read(String type, Path path, MappingSaveParameters saveParameters) throws MappingParseException, IOException {
		if (type.equals("enigma")) {
			return (Files.isDirectory(path) ? MappingFormat.ENIGMA_DIRECTORY : MappingFormat.ENIGMA_ZIP).read(path, ProgressListener.none(), saveParameters, null);
		}

		if (type.equals("tiny")) {
			return MappingFormat.TINY_FILE.read(path, ProgressListener.none(), saveParameters, null);
		}

		MappingFormat format = null;

		try {
			format = MappingFormat.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			if (type.equals("tinyv2")) {
				format = MappingFormat.TINY_V2;
			}
		}

		if (format != null) {
			return format.read(path, ProgressListener.none(), saveParameters, null);
		}

		throw new IllegalArgumentException("no reader for " + type);
	}

	public static void write(EntryTree<EntryMapping> mappings, String type, Path path, MappingSaveParameters saveParameters) {
		if (type.equals("enigma")) {
			MappingFormat.ENIGMA_DIRECTORY.write(mappings, path, ProgressListener.none(), saveParameters);
			return;
		}

		if (type.startsWith("tinyv2:") || type.startsWith("tiny_v2:")) {
			String[] split = type.split(":");

			if (split.length != 3) {
				throw new IllegalArgumentException("specify column names as 'tinyv2:from_namespace:to_namespace'");
			}

			if (!System.getProperty("enigma.use_mappingio", "true").equals("true")) {
				new TinyV2Writer(split[1], split[2]).write(mappings, path, ProgressListener.none(), saveParameters);
				return;
			}

			try {
				VisitableMappingTree tree = MappingIoConverter.toMappingIo(mappings, ProgressListener.none(), split[1], split[2]);
				tree.accept(MappingWriter.create(path, net.fabricmc.mappingio.format.MappingFormat.TINY_2_FILE));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			return;
		}

		if (type.startsWith("tiny:")) {
			String[] split = type.split(":");

			if (split.length != 3) {
				throw new IllegalArgumentException("specify column names as 'tiny:from_column:to_column'");
			}

			if (!System.getProperty("enigma.use_mappingio", "true").equals("true")) {
				new TinyMappingsWriter(split[1], split[2]).write(mappings, path, ProgressListener.none(), saveParameters);
				return;
			}

			try {
				VisitableMappingTree tree = MappingIoConverter.toMappingIo(mappings, ProgressListener.none(), split[1], split[2]);
				tree.accept(MappingWriter.create(path, net.fabricmc.mappingio.format.MappingFormat.TINY_FILE));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			return;
		}

		MappingFormat format = null;

		try {
			format = MappingFormat.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			// ignored
		}

		if (format != null) {
			format.write(mappings, path, ProgressListener.none(), saveParameters);
			return;
		}

		throw new IllegalArgumentException("no writer for " + type);
	}
}
