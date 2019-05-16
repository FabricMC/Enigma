package cuchaz.enigma.command;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class CheckMappingsCommand extends Command {

	public CheckMappingsCommand() {
		super("checkmappings");
	}

	@Override
	public String getUsage() {
		return "<in jar> <mappings file>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 2;
	}

	@Override
	public void run(String... args) throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 0, "in jar", true));
		Path fileMappings = getReadablePath(getArg(args, 1, "mappings file", true));

		System.out.println("Reading JAR...");
		Deobfuscator deobfuscator = new Deobfuscator(new JarFile(fileJarIn));
		System.out.println("Reading mappings...");

		MappingFormat format = chooseEnigmaFormat(fileMappings);
		EntryTree<EntryMapping> mappings = format.read(fileMappings, ProgressListener.VOID);
		deobfuscator.setMappings(mappings);

		JarIndex idx = deobfuscator.getJarIndex();

		boolean error = false;

		for (Set<ClassEntry> partition : idx.getPackageVisibilityIndex().getPartitions()) {
			long packages = partition.stream().map(deobfuscator.getMapper()::deobfuscate).map(ClassEntry::getPackageName).distinct().count();
			if (packages > 1) {
				error = true;
				System.err.println("ERROR: Must be in one package:\n" + partition.stream().map(deobfuscator.getMapper()::deobfuscate).map(ClassEntry::toString).sorted().collect(Collectors.joining("\n")));
			}
		}

		if (error) {
			throw new IllegalStateException("Errors in package visibility detected, see SysErr above");
		}
	}
}
