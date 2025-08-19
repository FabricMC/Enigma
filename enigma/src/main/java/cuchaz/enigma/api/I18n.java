package cuchaz.enigma.api;

public final class I18n {
	private I18n() {
	}

	public static String translate(String key) {
		return cuchaz.enigma.utils.I18n.translate(key);
	}

	public static String translate(String key, Object... args) {
		return cuchaz.enigma.utils.I18n.translateFormatted(key, args);
	}
}
