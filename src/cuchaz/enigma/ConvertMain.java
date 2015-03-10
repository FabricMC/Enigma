package cuchaz.enigma;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.jar.JarFile;

import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.FieldMatches;
import cuchaz.enigma.convert.MappingsConverter;
import cuchaz.enigma.convert.MatchesReader;
import cuchaz.enigma.convert.MatchesWriter;
import cuchaz.enigma.gui.ClassMatchingGui;
import cuchaz.enigma.gui.FieldMatchingGui;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsChecker;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;


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
		File classMatchesFile = new File(inMappingsFile.getName() + ".class.matches");
		File fieldMatchesFile = new File(inMappingsFile.getName() + ".field.matches");

		//computeClassMatches(classMatchesFile, sourceJar, destJar, mappings);
		editClasssMatches(classMatchesFile, sourceJar, destJar, mappings);
		//convertMappings(outMappingsFile, sourceJar, destJar, mappings, classMatchesFile);
		//computeFieldMatches(fieldMatchesFile, destJar, outMappingsFile, classMatchesFile);
		//editFieldMatches(sourceJar, destJar, outMappingsFile, mappings, classMatchesFile, fieldMatchesFile);
		
		/* TODO
		// write out the converted mappings
		FileWriter writer = new FileWriter(outMappingsFile);
		new MappingsWriter().write(writer, mappings);
		writer.close();
		System.out.println("Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath());
		*/
	}
	
	private static void computeClassMatches(File classMatchesFile, JarFile sourceJar, JarFile destJar, Mappings mappings)
	throws IOException {
		ClassMatches classMatches = MappingsConverter.computeClassMatches(sourceJar, destJar, mappings);
		MatchesWriter.writeClasses(classMatches, classMatchesFile);
		System.out.println("Wrote:\n\t" + classMatchesFile.getAbsolutePath());
	}
	
	private static void editClasssMatches(final File classMatchesFile, JarFile sourceJar, JarFile destJar, Mappings mappings)
	throws IOException {
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(mappings);
		System.out.println("Starting GUI...");
		new ClassMatchingGui(classMatches, deobfuscators.source, deobfuscators.dest).setSaveListener(new ClassMatchingGui.SaveListener() {
			@Override
			public void save(ClassMatches matches) {
				try {
					MatchesWriter.writeClasses(matches, classMatchesFile);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		});
	}
	
	private static void convertMappings(File outMappingsFile, JarFile sourceJar, JarFile destJar, Mappings mappings, File classMatchesFile)
	throws IOException {
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(mappings);
		
		Mappings newMappings = MappingsConverter.newMappings(classMatches, mappings, deobfuscators.source, deobfuscators.source);
		
		try (FileWriter out = new FileWriter(outMappingsFile)) {
			new MappingsWriter().write(out, newMappings);
		}
		System.out.println("Write converted mappings to: " + outMappingsFile.getAbsolutePath());
	}
	
	private static void computeFieldMatches(File fieldMatchesFile, JarFile destJar, File destMappingsFile, File classMatchesFile)
	throws IOException, MappingParseException {
		
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		System.out.println("Reading mappings...");
		Mappings destMappings = new MappingsReader().read(new FileReader(destMappingsFile));
		System.out.println("Indexing dest jar...");
		Deobfuscator destDeobfuscator = new Deobfuscator(destJar);
		
		System.out.println("Writing field matches...");
		
		// get the matched and unmatched field mappings
		FieldMatches fieldMatches = MappingsConverter.computeFieldMatches(destDeobfuscator, destMappings, classMatches);
		
		MatchesWriter.writeFields(fieldMatches, fieldMatchesFile);
		System.out.println("Wrote:\n\t" + fieldMatchesFile.getAbsolutePath());
	}
	
	private static void editFieldMatches(JarFile sourceJar, JarFile destJar, File destMappingsFile, Mappings sourceMappings, File classMatchesFile, final File fieldMatchesFile)
	throws IOException, MappingParseException {
		
		System.out.println("Reading matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		FieldMatches fieldMatches = MatchesReader.readFields(fieldMatchesFile);
		
		// prep deobfuscators
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(sourceMappings);
		Mappings destMappings = new MappingsReader().read(new FileReader(destMappingsFile));
		MappingsChecker checker = new MappingsChecker(deobfuscators.dest.getJarIndex());
		checker.dropBrokenMappings(destMappings);
		deobfuscators.dest.setMappings(destMappings);
		
		new FieldMatchingGui(classMatches, fieldMatches, deobfuscators.source, deobfuscators.dest).setSaveListener(new FieldMatchingGui.SaveListener() {
			@Override
			public void save(FieldMatches matches) {
				try {
					MatchesWriter.writeFields(matches, fieldMatchesFile);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		});
	}
	
	private static class Deobfuscators {
		
		public Deobfuscator source;
		public Deobfuscator dest;
		
		public Deobfuscators(JarFile sourceJar, JarFile destJar) {
			System.out.println("Indexing source jar...");
			IndexerThread sourceIndexer = new IndexerThread(sourceJar);
			sourceIndexer.start();
			System.out.println("Indexing dest jar...");
			IndexerThread destIndexer = new IndexerThread(destJar);
			destIndexer.start();
			sourceIndexer.joinOrBail();
			destIndexer.joinOrBail();
			source = sourceIndexer.deobfuscator;
			dest = destIndexer.deobfuscator;
		}
	}
	
	private static class IndexerThread extends Thread {
		
		private JarFile m_jarFile;
		public Deobfuscator deobfuscator;
		
		public IndexerThread(JarFile jarFile) {
			m_jarFile = jarFile;
			deobfuscator = null;
		}
		
		public void joinOrBail() {
			try {
				join();
			} catch (InterruptedException ex) {
				throw new Error(ex);
			}
		}

		@Override
		public void run() {
			try {
				deobfuscator = new Deobfuscator(m_jarFile);
			} catch (IOException ex) {
				throw new Error(ex);
			}
		}
	}
}
