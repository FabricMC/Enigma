package cuchaz.enigma.translation.representation.entry;

import cuchaz.enigma.translation.representation.AccessFlags;

public interface DefEntry<P extends Entry<?>> extends Entry<P> {
	AccessFlags getAccess();
}
