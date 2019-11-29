package cuchaz.enigma.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.EnigmaMappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;


/**
 * This is a tool to mass-migrate enigma mappings to a newer format, to avoid creating unnecessary diffs when a file is first changed.
 */
public class RefreshEnigmaMappingsCommand extends Command {
	public RefreshEnigmaMappingsCommand() {
		super("refresh-enigma-mappings");
	}

	@Override
	public String getUsage() {
		return "<mappings directory>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 1;
	}

	@Override
	public void run(String... args) throws Exception {
		Path directory = Paths.get(args[0]);
		if (!Files.exists(directory) || !Files.isDirectory(directory)) {
			throw new IllegalArgumentException(directory + " is not a valid directory.");
		}
		EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(
						directory, ProgressListener.none(), new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF)
		);
		EnigmaMappingsWriter.DIRECTORY.write(
						mappings, directory, ProgressListener.none(), new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF)
		);

	}
}
