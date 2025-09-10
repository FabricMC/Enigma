package cuchaz.enigma.api.view;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.api.DataInvalidationEvent;
import cuchaz.enigma.api.DataInvalidationListener;
import cuchaz.enigma.api.view.entry.EntryView;

public interface ProjectView {
	<T extends EntryView> T deobfuscate(T entry);

	void addDataInvalidationListener(DataInvalidationListener listener);

	default void invalidateData(DataInvalidationEvent.InvalidationType type) {
		invalidateData((Collection<String>) null, type);
	}

	default void invalidateData(String className, DataInvalidationEvent.InvalidationType type) {
		invalidateData(List.of(className), type);
	}

	void invalidateData(@Nullable Collection<String> classes, DataInvalidationEvent.InvalidationType type);
}
