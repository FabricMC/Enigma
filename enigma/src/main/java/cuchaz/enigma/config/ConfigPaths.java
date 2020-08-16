package cuchaz.enigma.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import cuchaz.enigma.utils.Os;

public class ConfigPaths {

	public static Path getConfigFilePath(String name) {
		String fileName = Os.getOs() == Os.LINUX ? String.format("%src", name) : String.format("%s.ini", name);
		return getConfigPathRoot().resolve(fileName);
	}

	public static Path getConfigPathRoot() {
		switch (Os.getOs()) {
			case LINUX:
				String configHome = System.getenv("XDG_CONFIG_HOME");
				if (configHome == null) {
					return getUserHomeUnix().resolve(".config");
				}
				return Paths.get(configHome);
			case MAC:
				return getUserHomeUnix().resolve("Library").resolve("Application Support");
			case WINDOWS:
				return Paths.get(System.getenv("LOCALAPPDATA"));
			default:
				return Paths.get(System.getProperty("user.dir"));
		}
	}

	private static Path getUserHomeUnix() {
		String userHome = System.getenv("HOME");
		if (userHome == null) {
			userHome = System.getProperty("user.dir");
		}
		return Paths.get(userHome);
	}

}
