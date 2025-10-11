package cuchaz.enigma.api.view.index;

public interface JarIndexView {
	EntryIndexView getEntryIndex();
	InheritanceIndexView getInheritanceIndex();
	ReferenceIndexView getReferenceIndex();
	BridgeMethodIndexView getBridgeMethodIndex();
}
