package cuchaz.enigma.analysis;

import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.Decompilers;

public final class BuiltinPlugin implements EnigmaPlugin {
	@Override
	public void init(EnigmaPluginContext ctx) {
		registerDecompilerServices(ctx);
	}

	private void registerDecompilerServices(EnigmaPluginContext ctx) {
		ctx.registerService("enigma:vineflower", DecompilerService.TYPE, () -> Decompilers.VINEFLOWER);
		ctx.registerService("enigma:cfr", DecompilerService.TYPE, () -> Decompilers.CFR);
		ctx.registerService("enigma:procyon", DecompilerService.TYPE, () -> Decompilers.PROCYON);
		ctx.registerService("enigma:bytecode", DecompilerService.TYPE, () -> Decompilers.BYTECODE);
	}
}
