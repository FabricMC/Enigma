package cuchaz.enigma.source;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;

public interface DecompilerService extends EnigmaService {
    EnigmaServiceType<DecompilerService> TYPE = EnigmaServiceType.create("decompiler");

    Decompiler create(ClassProvider classProvider, SourceSettings settings);
}
