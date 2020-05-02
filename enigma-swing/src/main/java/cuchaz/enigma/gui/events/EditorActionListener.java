package cuchaz.enigma.gui.events;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.gui.util.ClassHandle;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface EditorActionListener {

	default void onCursorReferenceChanged(PanelEditor editor, EntryReference<Entry<?>, Entry<?>> ref) {
	}

	default void onClassHandleChanged(PanelEditor editor, ClassEntry old, ClassHandle ch) {
	}

	default void onTitleChanged(PanelEditor editor, String title) {
	}

}
