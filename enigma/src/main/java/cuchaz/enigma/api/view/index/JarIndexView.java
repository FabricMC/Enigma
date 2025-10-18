package cuchaz.enigma.api.view.index;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface JarIndexView {
	EntryIndexView getEntryIndex();
	InheritanceIndexView getInheritanceIndex();
	ReferenceIndexView getReferenceIndex();
	BridgeMethodIndexView getBridgeMethodIndex();
}
