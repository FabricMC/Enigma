package cuchaz.enigma.api.view.entry;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface LocalVariableDefEntryView extends LocalVariableEntryView {
	String getDescriptor();
}
