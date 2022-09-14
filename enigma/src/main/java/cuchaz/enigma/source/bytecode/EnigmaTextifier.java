package cuchaz.enigma.source.bytecode;

import org.objectweb.asm.util.Textifier;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.source.SourceIndex;

public class EnigmaTextifier extends Textifier {
	private final SourceIndex sourceIndex;

	public EnigmaTextifier(SourceIndex sourceIndex) {
		super(Enigma.ASM_VERSION);
		this.sourceIndex = sourceIndex;
	}
}
