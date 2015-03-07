package cuchaz.enigma;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.jar.JarFile;

import cuchaz.enigma.convert.MappingsConverter;
import cuchaz.enigma.convert.Matches;
import cuchaz.enigma.convert.MatchesReader;
import cuchaz.enigma.convert.MatchesWriter;
import cuchaz.enigma.gui.MatchingGui;
import cuchaz.enigma.gui.MatchingGui.SaveListener;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;


public class ConvertMain {

	public static void main(String[] args)
	throws IOException, MappingParseException {
		
		// init files
		File home = new File(System.getProperty("user.home"));
		JarFile sourceJar = new JarFile(new File(home, ".minecraft/versions/1.8/1.8.jar"));
		JarFile destJar = new JarFile(new File(home, ".minecraft/versions/1.8.3/1.8.3.jar"));
		File inMappingsFile = new File("../Enigma Mappings/1.8.mappings");
		File outMappingsFile = new File("../Enigma Mappings/1.8.3.mappings");
		Mappings mappings = new MappingsReader().read(new FileReader(inMappingsFile));
		File matchingFile = new File(inMappingsFile.getName() + ".matching");

		//computeMatches(matchingFile, sourceJar, destJar, mappings);
		editMatches(matchingFile, sourceJar, destJar, mappings);
		//convertMappings(outMappingsFile, mappings, matchingFile);
		
		/* TODO
		// write out the converted mappings
		FileWriter writer = new FileWriter(outMappingsFile);
		new MappingsWriter().write(writer, mappings);
		writer.close();
		System.out.println("Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath());
		*/
	}
	
	private static void computeMatches(File matchingFile, JarFile sourceJar, JarFile destJar, Mappings mappings)
	throws IOException {
		Matches matches = MappingsConverter.computeMatches(sourceJar, destJar, mappings);
		MatchesWriter.write(matches, matchingFile);
		System.out.println("Wrote:\n\t" + matchingFile.getAbsolutePath());
	}
	
	private static void editMatches(final File matchingFile, JarFile sourceJar, JarFile destJar, Mappings mappings)
	throws IOException {
		System.out.println("Reading matches...");
		Matches matches = MatchesReader.read(matchingFile);
		System.out.println("Indexing source jar...");
		Deobfuscator sourceDeobfuscator = new Deobfuscator(sourceJar);
		sourceDeobfuscator.setMappings(mappings);
		System.out.println("Indexing dest jar...");
		Deobfuscator destDeobfuscator = new Deobfuscator(destJar);
		System.out.println("Starting GUI...");
		new MatchingGui(matches, sourceDeobfuscator, destDeobfuscator).setSaveListener(new SaveListener() {
			@Override
			public void save(Matches matches) {
				try {
					MatchesWriter.write(matches, matchingFile);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		});
	}
	
	private static void convertMappings(File outMappingsFile, Mappings mappings, File matchingFile)
	throws IOException {
		Matches matches = MatchesReader.read(matchingFile);
		MappingsConverter.convertMappings(mappings, matches.getUniqueMatches());
	}
}
