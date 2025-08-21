package cuchaz.enigma;

import java.util.List;
import java.util.Map;

import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;

public final class EnigmaServices {
	private final Map<EnigmaServiceType<?>, List<EnigmaService>> services;

	EnigmaServices(Map<EnigmaServiceType<?>, List<EnigmaService>> services) {
		this.services = services;
	}

	@SuppressWarnings("unchecked")
	public <T extends EnigmaService> List<T> get(EnigmaServiceType<T> type) {
		return (List<T>) services.get(type);
	}
}
