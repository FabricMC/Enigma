package cuchaz.enigma.network;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.utils.Utils;

public class DedicatedEnigmaServer extends EnigmaServer {
	private final EnigmaProfile profile;
	private final MappingFormat mappingFormat;
	private final Path mappingsFile;
	private final PrintWriter log;
	private BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();

	public DedicatedEnigmaServer(byte[] jarChecksum, char[] password, EnigmaProfile profile, MappingFormat mappingFormat, Path mappingsFile, PrintWriter log, EntryRemapper mappings, int port) {
		super(jarChecksum, password, mappings, port);
		this.profile = profile;
		this.mappingFormat = mappingFormat;
		this.mappingsFile = mappingsFile;
		this.log = log;
	}

	@Override
	protected void runOnThread(Runnable task) {
		tasks.add(task);
	}

	@Override
	public void log(String message) {
		super.log(message);
		log.println(message);
	}

	public static void main(String[] args) {
		OptionParser parser = new OptionParser();

		OptionSpec<Path> jarOpt = parser.accepts("jar", "Jar file to open at startup; if there are multiple jars, the order must be the same between the server and all clients").withRequiredArg().required().withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> librariesOpt = parser.accepts("library", "Library file used by the jar").withRequiredArg().withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> mappingsOpt = parser.accepts("mappings", "Mappings file to open at startup").withRequiredArg().required().withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> profileOpt = parser.accepts("profile", "Profile json to apply at startup").withRequiredArg().withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Integer> portOpt = parser.accepts("port", "Port to run the server on").withOptionalArg().ofType(Integer.class).defaultsTo(EnigmaServer.DEFAULT_PORT);

		OptionSpec<String> passwordOpt = parser.accepts("password", "The password to join the server").withRequiredArg().defaultsTo("");

		OptionSpec<Path> logFileOpt = parser.accepts("log", "The log file to write to").withRequiredArg().withValuesConvertedBy(PathConverter.INSTANCE).defaultsTo(Paths.get("log.txt"));

		OptionSet parsedArgs = parser.parse(args);
		List<Path> jars = parsedArgs.valuesOf(jarOpt);
		Path mappingsFile = parsedArgs.valueOf(mappingsOpt);
		Path profileFile = parsedArgs.valueOf(profileOpt);
		int port = parsedArgs.valueOf(portOpt);
		char[] password = parsedArgs.valueOf(passwordOpt).toCharArray();

		if (password.length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			System.err.println("Password too long, must be at most " + EnigmaServer.MAX_PASSWORD_LENGTH + " characters");
			System.exit(1);
		}

		Path logFile = parsedArgs.valueOf(logFileOpt);

		System.out.println("Starting Enigma server");
		DedicatedEnigmaServer server;

		try {
			byte[] checksum = Utils.zipSha1(jars.toArray(new Path[0]));

			EnigmaProfile profile = EnigmaProfile.read(profileFile);
			Enigma enigma = Enigma.builder().setProfile(profile).build();
			System.out.println("Indexing Jar...");
			EnigmaProject project = enigma.openJars(jars, parsedArgs.valuesOf(librariesOpt), ProgressListener.none());

			MappingFormat mappingFormat = MappingFormat.ENIGMA_DIRECTORY;
			EntryRemapper mappings;

			if (!Files.exists(mappingsFile)) {
				mappings = EntryRemapper.empty(project.getJarIndex());
			} else {
				System.out.println("Reading mappings...");

				if (Files.isDirectory(mappingsFile)) {
					mappingFormat = MappingFormat.ENIGMA_DIRECTORY;
				} else if (mappingsFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
					mappingFormat = MappingFormat.ENIGMA_ZIP;
				} else {
					mappingFormat = MappingFormat.ENIGMA_FILE;
				}

				mappings = EntryRemapper.mapped(project.getJarIndex(), mappingFormat.read(mappingsFile, ProgressListener.none(), profile.getMappingSaveParameters(), project.getJarIndex()));
			}

			PrintWriter log = new PrintWriter(Files.newBufferedWriter(logFile));

			server = new DedicatedEnigmaServer(checksum, password, profile, mappingFormat, mappingsFile, log, mappings, port);
			server.start();
			System.out.println("Server started");
		} catch (IOException | MappingParseException e) {
			System.err.println("Error starting server!");
			e.printStackTrace();
			System.exit(1);
			return;
		}

		// noinspection RedundantSuppression
		// noinspection Convert2MethodRef - javac 8 bug
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> server.runOnThread(() -> server.saveMappings()), 0, 1, TimeUnit.MINUTES);
		Runtime.getRuntime().addShutdownHook(new Thread(server::saveMappings));

		while (true) {
			try {
				server.tasks.take().run();
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	@Override
	public synchronized void stop() {
		super.stop();
		System.exit(0);
	}

	private void saveMappings() {
		mappingFormat.write(getMappings().getObfToDeobf(), getMappings().takeMappingDelta(), mappingsFile, ProgressListener.none(), profile.getMappingSaveParameters());
		log.flush();
	}

	public static class PathConverter implements ValueConverter<Path> {
		public static final ValueConverter<Path> INSTANCE = new PathConverter();

		PathConverter() {
		}

		@Override
		public Path convert(String path) {
			// expand ~ to the home dir
			if (path.startsWith("~")) {
				// get the home dir
				Path dirHome = Paths.get(System.getProperty("user.home"));

				// is the path just ~/ or is it ~user/ ?
				if (path.startsWith("~/")) {
					return dirHome.resolve(path.substring(2));
				} else {
					return dirHome.getParent().resolve(path.substring(1));
				}
			}

			return Paths.get(path);
		}

		@Override
		public Class<? extends Path> valueType() {
			return Path.class;
		}

		@Override
		public String valuePattern() {
			return "path";
		}
	}
}
