package cuchaz.enigma.api.view.entry;

public interface LocalVariableEntryView extends EntryView {
	int getIndex();

	boolean isArgument();

	MethodEntryView getParent();
}
