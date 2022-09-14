package cuchaz.enigma;

import java.util.List;

import com.google.common.collect.ImmutableListMultimap;

import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;

public final class EnigmaServices {
	private final ImmutableListMultimap<EnigmaServiceType<?>, EnigmaService> services;

	EnigmaServices(ImmutableListMultimap<EnigmaServiceType<?>, EnigmaService> services) {
		this.services = services;
	}

	@SuppressWarnings("unchecked")
	public <T extends EnigmaService> List<T> get(EnigmaServiceType<T> type) {
		return (List<T>) services.get(type);
	}
}
