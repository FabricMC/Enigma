package cuchaz.enigma.api.view.entry;

public interface MethodEntryView extends EntryView {
	String getDescriptor();

	ClassEntryView getParent();
}
