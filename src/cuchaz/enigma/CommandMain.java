/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma;

import java.io.File;
import java.io.FileReader;
import java.util.jar.JarFile;

import cuchaz.enigma.Deobfuscator.ProgressListener;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;

public class CommandMain {
	
	public static class ConsoleProgressListener implements ProgressListener {
		
		private static final int ReportTime = 5000; // 5s

		private int m_totalWork;
		private long m_startTime;
		private long m_lastReportTime;
		
		@Override
		public void init(int totalWork, String title) {
			m_totalWork = totalWork;
			m_startTime = System.currentTimeMillis();
			m_lastReportTime = m_startTime;
			System.out.println(title);
		}

		@Override
		public void onProgress(int numDone, String message) {
			
			long now = System.currentTimeMillis();
			boolean isLastUpdate = numDone == m_totalWork;
			boolean shouldReport = isLastUpdate || now - m_lastReportTime > ReportTime;
			
			if (shouldReport) {
				int percent = numDone*100/m_totalWork;
				System.out.println(String.format("\tProgress: %3d%%", percent));
				m_lastReportTime = now;
			}
			if (isLastUpdate) {
				double elapsedSeconds = (now - m_startTime)/1000;
				System.out.println(String.format("Finished in %.1f seconds", elapsedSeconds));
			}
		}
	}
	
	public static void main(String[] args)
	throws Exception {
		
		try {
			
			// process the command
			String command = getArg(args, 0, "command", true);
			if (command.equalsIgnoreCase("deobfuscate")) {
				deobfuscate(args);
			} else if (command.equalsIgnoreCase("decompile")) {
				decompile(args);
			} else if (command.equalsIgnoreCase("protectify")) {
				protectify(args);
			} else if (command.equalsIgnoreCase("publify")) {
				publify(args);
			} else {
				throw new IllegalArgumentException("Command not recognized: " + command);
			}
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println(String.format("%s - %s", Constants.Name, Constants.Version));
		System.out.println("Usage:");
		System.out.println("\tjava -cp enigma.jar cuchaz.enigma.CommandMain <command>");
		System.out.println("\twhere <command> is one of:");
		System.out.println("\t\tdeobfuscate <in jar> <out jar> [<mappings file>]");
		System.out.println("\t\tdecompile <in jar> <out folder> [<mappings file>]");
		System.out.println("\t\tprotectify <in jar> <out jar>");
	}
	
	private static void decompile(String[] args)
	throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 1, "in jar", true));
		File fileJarOut = getWritableFolder(getArg(args, 2, "out folder", true));
		File fileMappings = getReadableFile(getArg(args, 3, "mappings file", false));
		Deobfuscator deobfuscator = getDeobfuscator(fileMappings, new JarFile(fileJarIn));
		deobfuscator.writeSources(fileJarOut, new ConsoleProgressListener());
	}

	private static void deobfuscate(String[] args)
	throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 1, "in jar", true));
		File fileJarOut = getWritableFile(getArg(args, 2, "out jar", true));
		File fileMappings = getReadableFile(getArg(args, 3, "mappings file", false));
		Deobfuscator deobfuscator = getDeobfuscator(fileMappings, new JarFile(fileJarIn));
		deobfuscator.writeJar(fileJarOut, new ConsoleProgressListener());
	}
	
	private static void protectify(String[] args)
	throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 1, "in jar", true));
		File fileJarOut = getWritableFile(getArg(args, 2, "out jar", true));
		Deobfuscator deobfuscator = getDeobfuscator(null, new JarFile(fileJarIn));
		deobfuscator.protectifyJar(fileJarOut, new ConsoleProgressListener());
	}
	
	private static void publify(String[] args)
	throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 1, "in jar", true));
		File fileJarOut = getWritableFile(getArg(args, 2, "out jar", true));
		Deobfuscator deobfuscator = getDeobfuscator(null, new JarFile(fileJarIn));
		deobfuscator.publifyJar(fileJarOut, new ConsoleProgressListener());
	}
	
	private static Deobfuscator getDeobfuscator(File fileMappings, JarFile jar)
	throws Exception {
		System.out.println("Reading jar...");
		Deobfuscator deobfuscator = new Deobfuscator(jar);
		if (fileMappings != null) {
			System.out.println("Reading mappings...");
			Mappings mappings = new MappingsReader().read(new FileReader(fileMappings));
			deobfuscator.setMappings(mappings);
		}
		return deobfuscator;
	}
	
	private static String getArg(String[] args, int i, String name, boolean required) {
		if (i >= args.length) {
			if (required) {
				throw new IllegalArgumentException(name + " is required");
			} else {
				return null;
			}
		}
		return args[i];
	}

	private static File getWritableFile(String path) {
		if (path == null) {
			return null;
		}
		File file = new File(path).getAbsoluteFile();
		File dir = file.getParentFile();
		if (dir == null) {
			throw new IllegalArgumentException("Cannot write to folder: " + dir);
		}
		// quick fix to avoid stupid stuff in Gradle code
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		return file;
	}

	private static File getWritableFolder(String path) {
		if (path == null) {
			return null;
		}
		File dir = new File(path).getAbsoluteFile();
		if (!dir.exists()) {
			throw new IllegalArgumentException("Cannot write to folder: " + dir);
		}
		return dir;
	}
	
	private static File getReadableFile(String path) {
		if (path == null) {
			return null;
		}
		File file = new File(path).getAbsoluteFile();
		if (!file.exists()) {
			throw new IllegalArgumentException("Cannot find file: " + file.getAbsolutePath());
		}
		return file;
	}
}
