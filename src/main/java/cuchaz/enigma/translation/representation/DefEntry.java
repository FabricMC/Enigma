package cuchaz.enigma.translation.representation;

import cuchaz.enigma.bytecode.AccessFlags;

public interface DefEntry extends Entry {
    AccessFlags getAccess();
}
