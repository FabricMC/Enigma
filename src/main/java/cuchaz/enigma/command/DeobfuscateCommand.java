package cuchaz.enigma.command;

import cuchaz.enigma.Deobfuscator;

import java.io.File;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class DeobfuscateCommand extends Command {

	public DeobfuscateCommand() {
		super("deobfuscate");
	}

	@Override
	public String getUsage() {
		return "<in jar> <out jar> [<mappings file>]";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 2 || length == 3;
	}

	@Override
	public void run(String... args) throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 0, "in jar", true));
		File fileJarOut = getWritableFile(getArg(args, 1, "out jar", true));
		Path fileMappings = getReadablePath(getArg(args, 2, "mappings file", false));
		Deobfuscator deobfuscator = getDeobfuscator(fileMappings, new JarFile(fileJarIn));
		deobfuscator.writeTransformedJar(fileJarOut, new Command.ConsoleProgressListener());
	}
}
