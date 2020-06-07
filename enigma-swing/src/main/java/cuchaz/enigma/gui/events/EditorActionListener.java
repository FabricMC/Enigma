package cuchaz.enigma.gui.events;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.gui.panels.EditorPanel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface EditorActionListener {

	default void onCursorReferenceChanged(EditorPanel editor, EntryReference<Entry<?>, Entry<?>> ref) {
	}

	default void onClassHandleChanged(EditorPanel editor, ClassEntry old, ClassHandle ch) {
	}

	default void onTitleChanged(EditorPanel editor, String title) {
	}

}
