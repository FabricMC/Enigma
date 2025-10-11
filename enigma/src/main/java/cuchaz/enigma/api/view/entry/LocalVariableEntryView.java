package cuchaz.enigma.api.view.entry;

import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public interface LocalVariableEntryView extends EntryView {
	int getIndex();

	boolean isArgument();

	MethodEntryView getParent();

	static LocalVariableEntryView create(MethodEntryView parent, int index, boolean argument) {
		return new LocalVariableEntry((MethodEntry) parent, index, null, argument, null);
	}
}
