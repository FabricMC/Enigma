package cuchaz.enigma.mapping;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.EnigmaMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import org.junit.Assert;
import org.junit.Test;

public class TestUnnamedMappings {
	private static Path DIRECTORY;

	static {
		try {
			DIRECTORY = Paths.get(TestTinyV2InnerClasses.class.getResource("/emptyfield/").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testParseAndWrite() throws IOException, MappingParseException {
		ProgressListener progressListener = ProgressListener.none();
		MappingSaveParameters params = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(DIRECTORY, progressListener, params);

		Path writtenDir = DIRECTORY.getParent().resolve("written");
		Files.createDirectories(writtenDir);
		EnigmaMappingsWriter.FILE.write(mappings, writtenDir.resolve("written.mapping"), progressListener, params);
	}

	@Test
	public void testParseAndConvert() throws IOException, MappingParseException {
		ProgressListener progressListener = ProgressListener.none();
		MappingSaveParameters params = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(DIRECTORY, progressListener, params);

		Path writtenDir = DIRECTORY.getParent().resolve("written");
		Files.createDirectories(writtenDir);
		new TinyV2Writer("intermediary", "named")
						.write(mappings, DIRECTORY.resolve("convertedtiny.tiny"), progressListener, params);

	}
}
