package cuchaz.enigma.api.service;

import org.objectweb.asm.tree.ClassNode;

public interface DecompilerInputTransformerService extends EnigmaService {
	EnigmaServiceType<DecompilerInputTransformerService> TYPE = EnigmaServiceType.create("decompiler_input_transformer");

	void transform(ClassNode classNode);
}
