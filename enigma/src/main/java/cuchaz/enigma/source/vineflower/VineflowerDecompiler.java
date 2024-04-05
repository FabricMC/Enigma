package cuchaz.enigma.source.vineflower;

import org.checkerframework.checker.nullness.qual.Nullable;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;

public class VineflowerDecompiler implements Decompiler {
	private final ClassProvider classProvider;
	private final SourceSettings settings;

	public VineflowerDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.settings = sourceSettings;
		this.classProvider = classProvider;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		return new VineflowerSource(new VineflowerContextSource(classProvider, className), remapper, settings);
	}
}
