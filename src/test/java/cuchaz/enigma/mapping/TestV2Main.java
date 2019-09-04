package cuchaz.enigma.mapping;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestV2Main {
	public static void main(String... args) throws Exception {
		Path path = Paths.get(TestV2Main.class.getResource("/tinyV2InnerClasses/").toURI());

		MappingSaveParameters parameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

		EntryTree<EntryMapping> tree = EnigmaMappingsReader.DIRECTORY.read(path, ProgressListener.none(), parameters);

		new TinyV2Writer("obf", "deobf").write(tree, Paths.get("currentYarn.tiny"), ProgressListener.none(), parameters);
	}
}
