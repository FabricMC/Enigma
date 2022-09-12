package cuchaz.enigma.source;

import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;
import cuchaz.enigma.classprovider.ClassProvider;

public interface DecompilerService extends EnigmaService {
	EnigmaServiceType<DecompilerService> TYPE = EnigmaServiceType.create("decompiler");

	Decompiler create(ClassProvider classProvider, SourceSettings settings);
}
