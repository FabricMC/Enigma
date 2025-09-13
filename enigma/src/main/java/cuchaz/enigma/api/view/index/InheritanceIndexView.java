package cuchaz.enigma.api.view.index;

import java.util.Collection;

import cuchaz.enigma.api.view.entry.ClassEntryView;

public interface InheritanceIndexView {
	Collection<? extends ClassEntryView> getParents(ClassEntryView entry);
	Collection<? extends ClassEntryView> getChildren(ClassEntryView entry);
}
