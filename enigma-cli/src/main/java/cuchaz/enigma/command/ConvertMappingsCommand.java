package cuchaz.enigma.command;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.utils.Utils;

public class ConvertMappingsCommand extends Command {
	public ConvertMappingsCommand() {
		super("convert-mappings");
	}

	@Override
	public String getUsage() {
		return "<source-format> <source> <result-format> <result>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 4;
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

		EntryTree<EntryMapping> mappings = MappingCommandsUtil.read(args[0], Paths.get(args[1]), saveParameters);

		Path output = Paths.get(args[3]);
		Utils.delete(output);
		MappingCommandsUtil.write(mappings, args[2], output, saveParameters);
	}
}
