package cuchaz.enigma.command;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

public class ConvertMappingsCommand extends Command {

	public ConvertMappingsCommand() {
		super("convertmappings");
	}

	@Override
	public String getUsage() {
		return "<enigma mappings> <converted mappings> <ENIGMA_FILE|ENIGMA_DIRECTORY|SRG_FILE>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 3;
	}

	@Override
	public void run(String... args) throws Exception {
		Path fileMappings = getReadablePath(getArg(args, 0, "enigma mappings", true));
		File result = getWritableFile(getArg(args, 1, "converted mappings", true));
		String name = getArg(args, 2, "format desc", true);
		MappingFormat saveFormat;
		try {
			saveFormat = MappingFormat.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(name + "is not a valid mapping format!");
		}

		System.out.println("Reading mappings...");

		MappingFormat readFormat = chooseEnigmaFormat(fileMappings);
		EntryTree<EntryMapping> mappings = readFormat.read(fileMappings, new ConsoleProgressListener());
		System.out.println("Saving new mappings...");

		saveFormat.write(mappings, result.toPath(), new ConsoleProgressListener());
	}
}
