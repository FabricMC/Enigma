package cuchaz.enigma.command;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;

import java.nio.file.Path;

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
		Path fileJarIn = getReadableFile(getArg(args, 0, "in jar", true)).toPath();
		Path fileJarOut = getWritableFolder(getArg(args, 1, "out folder", true)).toPath();
		Path fileMappings = getReadablePath(getArg(args, 2, "mappings file", false));

		EnigmaProject project = openProject(fileJarIn, fileMappings);

		ProgressListener progress = new ConsoleProgressListener();

		EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
		EnigmaProject.SourceExport source = jar.decompile(progress);

		source.write(fileJarOut, progress);
	}
}
