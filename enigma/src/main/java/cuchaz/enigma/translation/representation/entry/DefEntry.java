package cuchaz.enigma.translation.representation.entry;

import cuchaz.enigma.api.view.entry.DefEntryView;
import cuchaz.enigma.translation.representation.AccessFlags;

public interface DefEntry<P extends Entry<?>> extends Entry<P>, DefEntryView {
	AccessFlags getAccess();

	@Override
	default int getAccessFlags() {
		return getAccess().getFlags();
	}
}
