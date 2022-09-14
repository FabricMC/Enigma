package cuchaz.enigma.utils;

import java.util.Locale;

public enum Os {
	LINUX,
	MAC,
	SOLARIS,
	WINDOWS,
	OTHER;

	private static Os os = null;

	public static Os getOs() {
		if (os == null) {
			String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

			if (osName.contains("mac") || osName.contains("darwin")) {
				os = MAC;
			} else if (osName.contains("win")) {
				os = WINDOWS;
			} else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
				os = LINUX;
			} else if (osName.contains("sunos")) {
				os = SOLARIS;
			} else {
				os = OTHER;
			}
		}

		return os;
	}
}
