package cuchaz.enigma.api.service;

import cuchaz.enigma.translation.representation.entry.Entry;

public interface ObfuscationTestService extends EnigmaService {
	EnigmaServiceType<ObfuscationTestService> TYPE = EnigmaServiceType.create("obfuscation_test");

	boolean testDeobfuscated(Entry<?> entry);
}
