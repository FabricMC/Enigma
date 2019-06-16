package cuchaz.enigma;

import com.google.common.collect.ImmutableMap;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;

import java.util.Optional;

public final class EnigmaServices {
	private final ImmutableMap<EnigmaServiceType<?>, EnigmaService> services;

	EnigmaServices(ImmutableMap<EnigmaServiceType<?>, EnigmaService> services) {
		this.services = services;
	}

	@SuppressWarnings("unchecked")
	public <T extends EnigmaService> Optional<T> get(EnigmaServiceType<T> type) {
		EnigmaService service = services.get(type);
		return Optional.ofNullable((T) service);
	}
}
