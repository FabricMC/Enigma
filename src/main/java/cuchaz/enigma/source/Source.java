package cuchaz.enigma.source;

import cuchaz.enigma.translation.mapping.EntryRemapper;

public interface Source {
    String asString();

    Source addJavadocs(EntryRemapper remapper);

    SourceIndex index();
}
