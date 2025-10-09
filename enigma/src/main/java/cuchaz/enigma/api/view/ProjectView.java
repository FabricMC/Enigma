package cuchaz.enigma.api.view;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.api.DataInvalidationEvent;
import cuchaz.enigma.api.DataInvalidationListener;
import cuchaz.enigma.api.view.entry.EntryView;
import cuchaz.enigma.api.view.index.JarIndexView;

public interface ProjectView {
	<T extends EntryView> T deobfuscate(T entry);

	/**
	 * Must call {@link #registerForInverseMappings()} before using this method.
	 */
	<T extends EntryView> T obfuscate(T entry);

	void registerForInverseMappings();

	JarIndexView getJarIndex();

	Collection<String> getProjectClasses();

	Collection<String> getProjectAndLibraryClasses();

	@Nullable
	ClassNode getBytecode(String className);

	void addDataInvalidationListener(DataInvalidationListener listener);

	default void invalidateData(DataInvalidationEvent.InvalidationType type) {
		invalidateData((Collection<String>) null, type);
	}

	default void invalidateData(String className, DataInvalidationEvent.InvalidationType type) {
		invalidateData(List.of(className), type);
	}

	void invalidateData(@Nullable Collection<String> classes, DataInvalidationEvent.InvalidationType type);
}
