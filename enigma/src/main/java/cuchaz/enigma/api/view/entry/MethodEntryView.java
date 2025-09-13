package cuchaz.enigma.api.view.entry;

import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public interface MethodEntryView extends EntryView {
	String getDescriptor();

	ClassEntryView getParent();

	static MethodEntryView create(String className, String methodName, String descriptor) {
		return new MethodEntry(new ClassEntry(className), methodName, new MethodDescriptor(descriptor));
	}
}
