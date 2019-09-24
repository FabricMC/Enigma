package cuchaz.enigma;

import com.google.common.collect.ImmutableListMultimap;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;

import java.util.Collections;
import java.util.List;

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
