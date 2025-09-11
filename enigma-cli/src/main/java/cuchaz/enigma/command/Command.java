package cuchaz.enigma.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingIoConverter;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

public abstract class Command {
	public final String name;

	protected Command(String name) {
		this.name = name;
	}

	public abstract String getUsage();

	public abstract boolean isValidArgument(int length);

	public abstract void run(String... args) throws Exception;

	protected static EnigmaProject openProject(Path fileJarIn, Path fileMappings, List<Path> libraries) throws Exception {
		ProgressListener progress = new ConsoleProgressListener();

		Enigma enigma = Enigma.create();

		System.out.println("Reading jar...");
		EnigmaProject project = enigma.openJar(fileJarIn, libraries, progress);

		if (fileMappings != null) {
			System.out.println("Reading mappings...");

			MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();
			EntryTree<EntryMapping> mappings = readMappings(fileMappings, progress, saveParameters);

			project.setMappings(mappings);
		}

		return project;
	}

	protected static EntryTree<EntryMapping> readMappings(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
		// Legacy
		if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
			return MappingFormat.ENIGMA_ZIP.read(path, progress, saveParameters, null);
		}

		net.fabricmc.mappingio.format.MappingFormat format = MappingReader.detectFormat(path);
		if (format == null) throw new IllegalArgumentException("Unknown mapping format!");

		VisitableMappingTree tree = new MemoryMappingTree();
		MappingReader.read(path, format, tree);
		return MappingIoConverter.fromMappingIo(tree, progress, null);
	}

	protected static File getWritableFile(String path) {
		if (path == null) {
			return null;
		}

		File file = new File(path).getAbsoluteFile();
		File dir = file.getParentFile();

		if (dir == null) {
			throw new IllegalArgumentException("Cannot write file: " + path);
		}

		// quick fix to avoid stupid stuff in Gradle code
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}

		return file;
	}

	protected static File getWritableFolder(String path) {
		if (path == null) {
			return null;
		}

		File dir = new File(path).getAbsoluteFile();

		if (!dir.exists()) {
			throw new IllegalArgumentException("Cannot write to folder: " + dir);
		}

		return dir;
	}

	protected static File getReadableFile(String path) {
		if (path == null) {
			return null;
		}

		File file = new File(path).getAbsoluteFile();

		if (!file.exists()) {
			throw new IllegalArgumentException("Cannot find file: " + file.getAbsolutePath());
		}

		return file;
	}

	protected static Path getReadablePath(String path) {
		if (path == null) {
			return null;
		}

		Path file = Paths.get(path).toAbsolutePath();

		if (!Files.exists(file)) {
			throw new IllegalArgumentException("Cannot find file: " + file.toString());
		}

		return file;
	}

	protected static String getArg(String[] args, int i, String name, boolean required) {
		if (i >= args.length) {
			if (required) {
				throw new IllegalArgumentException(name + " is required");
			} else {
				return null;
			}
		}

		return args[i];
	}

	protected static List<Path> getReadablePaths(String[] args, int startingFrom) {
		List<Path> paths = new ArrayList<>();

		for (int i = startingFrom; i < args.length; i++) {
			paths.add(getReadablePath(args[i]));
		}

		return paths;
	}

	public static class ConsoleProgressListener implements ProgressListener {
		private static final int ReportTime = 5000; // 5s

		private int totalWork;
		private long startTime;
		private long lastReportTime;

		@Override
		public void init(int totalWork, String title) {
			this.totalWork = totalWork;
			this.startTime = System.currentTimeMillis();
			this.lastReportTime = this.startTime;
			System.out.println(title);
		}

		@Override
		public void step(int numDone, String message) {
			long now = System.currentTimeMillis();
			boolean isLastUpdate = numDone == this.totalWork;
			boolean shouldReport = isLastUpdate || now - this.lastReportTime > ReportTime;

			if (shouldReport) {
				int percent = numDone * 100 / this.totalWork;
				System.out.println(String.format("\tProgress: %3d%%", percent));
				this.lastReportTime = now;
			}

			if (isLastUpdate) {
				double elapsedSeconds = (now - this.startTime) / 1000.0;
				System.out.println(String.format("Finished in %.1f seconds", elapsedSeconds));
			}
		}
	}
}
