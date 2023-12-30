package cuchaz.enigma.source;

import cuchaz.enigma.source.bytecode.BytecodeDecompiler;
import cuchaz.enigma.source.cfr.CfrDecompiler;
import cuchaz.enigma.source.jadx.JadxDecompiler;
import cuchaz.enigma.source.procyon.ProcyonDecompiler;

public class Decompilers {
	public static final DecompilerService PROCYON = ProcyonDecompiler::new;
	public static final DecompilerService CFR = CfrDecompiler::new;
	public static final DecompilerService JADX = JadxDecompiler::new;
	public static final DecompilerService BYTECODE = BytecodeDecompiler::new;
}
