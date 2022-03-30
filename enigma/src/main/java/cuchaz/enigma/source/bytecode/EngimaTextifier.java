package cuchaz.enigma.source.bytecode;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.source.SourceIndex;
import org.objectweb.asm.util.Textifier;

public class EngimaTextifier extends Textifier {
    private final SourceIndex sourceIndex;

    public EngimaTextifier(SourceIndex sourceIndex) {
        super(Enigma.ASM_VERSION);
        this.sourceIndex = sourceIndex;
    }
}
