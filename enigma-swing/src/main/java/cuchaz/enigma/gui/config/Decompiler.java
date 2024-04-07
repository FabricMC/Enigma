package cuchaz.enigma.gui.config;

import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.Decompilers;

public enum Decompiler {
	VINEFLOWER("Vineflower", Decompilers.VINEFLOWER),
	CFR("CFR", Decompilers.CFR),
	JADX("JADX", Decompilers.JADX),
	PROCYON("Procyon", Decompilers.PROCYON),
	BYTECODE("Bytecode", Decompilers.BYTECODE);

	public final DecompilerService service;
	public final String name;

	Decompiler(String name, DecompilerService service) {
		this.name = name;
		this.service = service;
	}
}
