package cuchaz.enigma.api.view.entry;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

@ApiStatus.NonExtendable
public interface LocalVariableEntryView extends EntryView {
	int getIndex();

	boolean isArgument();

	MethodEntryView getParent();

	static LocalVariableEntryView create(MethodEntryView parent, int index, String name, boolean argument) {
		return new LocalVariableEntry((MethodEntry) parent, index, name, argument, null);
	}
}
