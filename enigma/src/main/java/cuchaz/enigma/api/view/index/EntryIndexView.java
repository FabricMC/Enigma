package cuchaz.enigma.api.view.index;

import java.util.Collection;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.api.view.entry.ClassDefEntryView;
import cuchaz.enigma.api.view.entry.ClassEntryView;
import cuchaz.enigma.api.view.entry.EntryView;

@ApiStatus.NonExtendable
public interface EntryIndexView {
	boolean hasEntry(EntryView entry);
	int getAccess(EntryView entry);
	ClassDefEntryView getDefinition(ClassEntryView entry);
	Collection<? extends ClassEntryView> getClasses();
}
