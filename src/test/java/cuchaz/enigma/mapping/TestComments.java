package cuchaz.enigma.mapping;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import org.junit.Test;

public class TestComments {
    private static Path DIRECTORY;

    static {
        try {
            DIRECTORY = Paths.get(TestTinyV2InnerClasses.class.getResource("/comments/").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testParseAndWrite() throws IOException, MappingParseException {
        ProgressListener progressListener = ProgressListener.none();
        MappingSaveParameters params = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
        EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(
                        DIRECTORY, progressListener, params);

        new TinyV2Writer("intermediary", "named")
                        .write(mappings, DIRECTORY.resolve("convertedtiny.tiny"), progressListener, params);
    }

}