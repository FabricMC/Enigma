package cuchaz.enigma.api.view.index;

import java.util.Collection;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.api.view.entry.ClassEntryView;

@ApiStatus.NonExtendable
public interface InheritanceIndexView {
	Collection<? extends ClassEntryView> getParents(ClassEntryView entry);
	Collection<? extends ClassEntryView> getChildren(ClassEntryView entry);
}
