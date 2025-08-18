package cuchaz.enigma.api.service;

public interface EnigmaServiceFactory<T extends EnigmaService> {
	T create();
}
