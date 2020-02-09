package cuchaz.enigma.source;

import cuchaz.enigma.source.procyon.ProcyonDecompiler;

public class Decompilers {
    public static final DecompilerService PROCYON = ProcyonDecompiler::new;
}
