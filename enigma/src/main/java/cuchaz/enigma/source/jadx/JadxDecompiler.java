package cuchaz.enigma.source.jadx;

import org.checkerframework.checker.nullness.qual.Nullable;
import jadx.api.JadxArgs;
import jadx.api.impl.NoOpCodeCache;

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
		return new JadxSource(settings, createJadxArgs(), classProvider.get(className), mapper);
	}

	private JadxArgs createJadxArgs() {
		JadxArgs args = new JadxArgs();
		args.setCodeCache(NoOpCodeCache.INSTANCE);
		args.setShowInconsistentCode(true);
		args.setRenameValid(false);
		args.setThreadsCount(Runtime.getRuntime().availableProcessors() / 2);

		return args;
	}
}
