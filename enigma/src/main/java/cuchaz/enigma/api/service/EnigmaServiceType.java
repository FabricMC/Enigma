package cuchaz.enigma.api.service;

public final class EnigmaServiceType<T extends EnigmaService> {
	public final String key;

	private EnigmaServiceType(String key) {
		this.key = key;
	}

	public static <T extends EnigmaService> EnigmaServiceType<T> create(String key) {
		return new EnigmaServiceType<>(key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof EnigmaServiceType) {
			return ((EnigmaServiceType) obj).key.equals(key);
		}

		return false;
	}
}
