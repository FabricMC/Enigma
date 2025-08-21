package cuchaz.enigma.source;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.translation.mapping.EntryRemapper;

public interface Decompiler {
	@Deprecated // use remapper specific one for easy doc inclusion
	default Source getSource(String className) {
		return getSource(className, null);
	}

	Source getSource(String className, @Nullable EntryRemapper remapper);
}
