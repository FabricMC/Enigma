package cuchaz.enigma.source.jadx;

import jadx.api.JadxArgs;
import jadx.api.impl.NoOpCodeCache;
import org.checkerframework.checker.nullness.qual.Nullable;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;

public class JadxDecompiler implements Decompiler {
	private final SourceSettings settings;
	private final ClassProvider classProvider;

	public JadxDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.settings = sourceSettings;
		this.classProvider = classProvider;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper mapper) {
		JadxHelper jadxHelper = new JadxHelper();

		return new JadxSource(settings, mapperX -> createJadxArgs(mapperX, jadxHelper), classProvider.get(className), mapper, jadxHelper);
	}

	private JadxArgs createJadxArgs(EntryRemapper mapper, JadxHelper jadxHelper) {
		CustomJadxArgs args = new CustomJadxArgs();
		args.setCodeCache(NoOpCodeCache.INSTANCE);
		args.setShowInconsistentCode(true);
		args.setInlineAnonymousClasses(false);
		args.setInlineMethods(false);
		args.setRespectBytecodeAccModifiers(true);
		args.setRenameValid(false);
		args.mapper = mapper;
		args.jadxHelper = jadxHelper;

		if (settings.removeImports) {
			// Commented out for now, since JADX would use full identifiers everywhere
			// args.setUseImports(false);
		}

		return args;
	}
}
