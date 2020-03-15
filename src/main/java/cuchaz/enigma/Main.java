/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;

import joptsimple.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.io.MoreFiles;

public class Main {

	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();

		OptionSpec<Path> jar = parser.accepts("jar", "Jar file to open at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> mappings = parser.accepts("mappings", "Mappings file to open at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> profile = parser.accepts("profile", "Profile json to apply at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		parser.accepts("help", "Displays help information");

		try {
			OptionSet options = parser.parse(args);

			if (options.has("help")) {
				parser.printHelpOn(System.out);
				return;
			}

			EnigmaProfile parsedProfile;
			if (options.has(profile)) {
				Path profilePath = options.valueOf(profile);
				try (BufferedReader reader = Files.newBufferedReader(profilePath)) {
					parsedProfile = EnigmaProfile.parse(reader);
				}
			} else {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/profile.json"), StandardCharsets.UTF_8))){
					parsedProfile = EnigmaProfile.parse(reader);
				} catch (IOException ex) {
					System.out.println("Failed to load default profile, will use empty profile: " + ex.getMessage());
					parsedProfile = EnigmaProfile.EMPTY;
				}
			}

			Gui gui = new Gui(parsedProfile);
			GuiController controller = gui.getController();

			if (options.has(jar)) {
				Path jarPath = options.valueOf(jar);
				controller.openJar(jarPath)
						.whenComplete((v, t) -> {
							if (options.has(mappings)) {
								Path mappingsPath = options.valueOf(mappings);
								if (Files.isDirectory(mappingsPath)) {
									controller.openMappings(MappingFormat.ENIGMA_DIRECTORY, mappingsPath);
								} else if ("zip".equalsIgnoreCase(MoreFiles.getFileExtension(mappingsPath))) {
									controller.openMappings(MappingFormat.ENIGMA_ZIP, mappingsPath);
								} else {
									controller.openMappings(MappingFormat.ENIGMA_FILE, mappingsPath);
								}
							}
						});
			}
		} catch (OptionException e) {
			System.out.println("Invalid arguments: " + e.getMessage());
			System.out.println();
			parser.printHelpOn(System.out);
		}
	}

	private static class PathConverter implements ValueConverter<Path> {
		static final ValueConverter<Path> INSTANCE = new PathConverter();

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
