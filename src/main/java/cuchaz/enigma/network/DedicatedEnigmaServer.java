package cuchaz.enigma.network;

import com.google.common.io.MoreFiles;
import cuchaz.enigma.*;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.Utils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class DedicatedEnigmaServer extends EnigmaServer {

	private final EnigmaProfile profile;
	private final MappingFormat mappingFormat;
	private final Path mappingsFile;
	private final PrintWriter log;
	private BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();

	public DedicatedEnigmaServer(
			byte[] jarChecksum,
			EnigmaProfile profile,
			MappingFormat mappingFormat,
			Path mappingsFile,
			PrintWriter log,
			EntryRemapper mappings,
			int port
	) {
		super(jarChecksum, mappings, port);
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

		OptionSpec<Path> jarOpt = parser.accepts("jar", "Jar file to open at startup")
				.withRequiredArg()
				.required()
				.withValuesConvertedBy(Main.PathConverter.INSTANCE);

		OptionSpec<Path> mappingsOpt = parser.accepts("mappings", "Mappings file to open at startup")
				.withRequiredArg()
				.required()
				.withValuesConvertedBy(Main.PathConverter.INSTANCE);

		OptionSpec<Path> profileOpt = parser.accepts("profile", "Profile json to apply at startup")
				.withRequiredArg()
				.withValuesConvertedBy(Main.PathConverter.INSTANCE);

		OptionSpec<Integer> portOpt = parser.accepts("port", "Port to run the server on")
				.withOptionalArg()
				.ofType(Integer.class)
				.defaultsTo(EnigmaServer.DEFAULT_PORT);

		OptionSet parsedArgs = parser.parse(args);
		Path jar = parsedArgs.valueOf(jarOpt);
		Path mappingsFile = parsedArgs.valueOf(mappingsOpt);
		Path profileFile = parsedArgs.valueOf(profileOpt);
		int port = parsedArgs.valueOf(portOpt);

		System.out.println("Starting Enigma server");
		DedicatedEnigmaServer server;
		try {
			byte[] checksum = Utils.sha1(parsedArgs.valueOf(jarOpt));

			EnigmaProfile profile = EnigmaProfile.read(profileFile);
			Enigma enigma = Enigma.builder().setProfile(profile).build();
			System.out.println("Indexing Jar...");
			EnigmaProject project = enigma.openJar(jar, ProgressListener.none());

			MappingFormat mappingFormat = MappingFormat.ENIGMA_DIRECTORY;
			EntryRemapper mappings;
			if (!Files.exists(mappingsFile)) {
				mappings = EntryRemapper.empty(project.getJarIndex());
			} else {
				System.out.println("Reading mappings...");
				if (Files.isDirectory(mappingsFile)) {
					mappingFormat = MappingFormat.ENIGMA_DIRECTORY;
				} else if ("zip".equalsIgnoreCase(MoreFiles.getFileExtension(mappingsFile))) {
					mappingFormat = MappingFormat.ENIGMA_ZIP;
				} else {
					mappingFormat = MappingFormat.ENIGMA_FILE;
				}
				mappings = EntryRemapper.mapped(project.getJarIndex(), mappingFormat.read(mappingsFile, ProgressListener.none(), profile.getMappingSaveParameters()));
			}

			PrintWriter log = new PrintWriter(Files.newBufferedWriter(Paths.get("log.txt")));

			server = new DedicatedEnigmaServer(checksum, profile, mappingFormat, mappingsFile, log, mappings, port);
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
}
