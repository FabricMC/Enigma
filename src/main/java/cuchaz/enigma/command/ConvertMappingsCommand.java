package cuchaz.enigma.command;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.serde.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConvertMappingsCommand extends Command {

	private final Map<String, FormatDesc> formatDescs = new HashMap<>();

	public ConvertMappingsCommand() {
		super("convertmappings");
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getWriter() != null) {
				formatDescs.put(format.name(), (args) -> format.getWriter());
			}
		}

		formatDescs.put(MappingFormat.TINY_V2_FILE.name(), (args) -> {
			String obfHeader = getArg(args, 3, "obfHeader", false);
			String deobfHeader = getArg(args, 4, "deobfHeader", false);
			return new TinyV2Writer(obfHeader == null ? "obf" : obfHeader, deobfHeader == null ? "deobf" : deobfHeader);
		});
	}

	@Override
	public String getUsage() {
		return "<enigma mappings> <converted mappings> <format desc> [<extra args>]";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length >= 3;
	}

	@Override
	public void run(String... args) throws Exception {
		Path fileMappings = getReadablePath(getArg(args, 0, "enigma mappings", true));
		File result = getWritableFile(getArg(args, 1, "converted mappings", true));
		String name = getArg(args, 2, "format desc", true);
		MappingsWriter saveFormat;
		try {
			saveFormat = formatDescs.get(name.toUpperCase(Locale.ROOT)).getMappingsWriter(args);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(name + "is not a valid mapping format!");
		}

		System.out.println("Reading mappings...");

		MappingFormat readFormat = chooseEnigmaFormat(fileMappings);
		EntryTree<EntryMapping> mappings = readFormat.read(fileMappings, new ConsoleProgressListener());
		System.out.println("Saving new mappings...");

		saveFormat.write(mappings, result.toPath(), new ConsoleProgressListener());
	}

	interface FormatDesc {
		MappingsWriter getMappingsWriter(String... args);
	}
}
