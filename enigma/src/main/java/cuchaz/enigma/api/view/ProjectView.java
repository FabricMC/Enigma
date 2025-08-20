package cuchaz.enigma.api.view;

import cuchaz.enigma.api.view.entry.EntryView;

public interface ProjectView {
	<T extends EntryView> T deobfuscate(T entry);
}
