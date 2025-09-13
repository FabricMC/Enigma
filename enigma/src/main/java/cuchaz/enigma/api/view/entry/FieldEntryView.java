package cuchaz.enigma.api.view.entry;

import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;

public interface FieldEntryView extends EntryView {
	String getDescriptor();

	ClassEntryView getParent();

	static FieldEntryView create(String className, String fieldName, String descriptor) {
		return new FieldEntry(new ClassEntry(className), fieldName, new TypeDescriptor(descriptor));
	}
}
