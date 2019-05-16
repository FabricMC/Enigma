package cuchaz.enigma.command;

import cuchaz.enigma.Deobfuscator;

import java.io.File;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class DecompileCommand extends Command {

	public DecompileCommand() {
		super("decompile");
	}

	@Override
	public String getUsage() {
		return "<in jar> <out folder> [<mappings file>]";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 2 || length == 3;
	}

	@Override
	public void run(String... args) throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 0, "in jar", true));
		File fileJarOut = getWritableFolder(getArg(args, 1, "out folder", true));
		Path fileMappings = getReadablePath(getArg(args, 2, "mappings file", false));
		Deobfuscator deobfuscator = getDeobfuscator(fileMappings, new JarFile(fileJarIn));
		deobfuscator.writeSources(fileJarOut.toPath(), new Command.ConsoleProgressListener());
	}
}
