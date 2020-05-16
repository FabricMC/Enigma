package cuchaz.enigma.source.cfr;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageInformation;

public class CfrSource implements Source {
    private final ClassFile tree;
    private final SourceIndex index;
    private final String string;

    public CfrSource(ClassFile tree, DCCommonState state, TypeUsageInformation typeUsages) {
        this.tree = tree;

        EnigmaDumper dumper = new EnigmaDumper(typeUsages);
        tree.dump(state.getObfuscationMapping().wrap(dumper));
        index = dumper.getIndex();
        string = dumper.getString();
    }

    @Override
    public String asString() {
        return string;
    }

    @Override
    public Source addJavadocs(EntryRemapper remapper) {
        return this; // TODO
    }

    @Override
    public SourceIndex index() {
        return index;
    }
}
