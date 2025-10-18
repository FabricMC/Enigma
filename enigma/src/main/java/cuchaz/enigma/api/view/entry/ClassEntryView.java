package cuchaz.enigma.api.view.entry;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

@ApiStatus.NonExtendable
public interface ClassEntryView extends EntryView {
	ClassEntryView getParent();

	static ClassEntryView create(String className) {
		return new ClassEntry(className);
	}
}
