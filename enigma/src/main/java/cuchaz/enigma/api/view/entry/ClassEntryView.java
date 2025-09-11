package cuchaz.enigma.api.view.entry;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

public interface ClassEntryView extends EntryView {
	ClassEntryView getParent();

	static ClassEntryView create(String className) {
		return new ClassEntry(className);
	}
}
