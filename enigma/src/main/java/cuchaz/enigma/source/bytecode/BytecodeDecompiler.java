package cuchaz.enigma.source.bytecode;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;

public class BytecodeDecompiler implements Decompiler {
	private final ClassProvider classProvider;

	public BytecodeDecompiler(ClassProvider classProvider, SourceSettings settings) {
		this.classProvider = classProvider;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		return new BytecodeSource(classProvider.get(className), remapper);
	}
}
