package cuchaz.enigma.source.cfr;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CfrSource implements Source {
    private final SourceSettings settings;
    private final ClassFile tree;
    private final DCCommonState state;
    private final TypeUsageInformation typeUsage;
    private final Options options;
    private final EntryRemapper mapper;

    private SourceIndex index;

    public CfrSource(SourceSettings settings, ClassFile tree, DCCommonState state, TypeUsageInformation typeUsage, Options options, @Nullable EntryRemapper mapper) {
        this.settings = settings;
        this.tree = tree;
        this.state = state;
        this.typeUsage = typeUsage;
        this.options = options;
        this.mapper = mapper;
    }

    @Override
    public Source withJavadocs(EntryRemapper mapper) {
        return new CfrSource(settings, tree, state, typeUsage, options, mapper);
    }

    @Override
    public SourceIndex index() {
        ensureDecompiled();
        return index;
    }

    @Override
    public String asString() {
        ensureDecompiled();
        return index.getSource();
    }

    private synchronized void ensureDecompiled() {
        if (index != null) {
            return;
        }

        EnigmaDumper dumper = new EnigmaDumper(new StringBuilder(), settings, typeUsage, options, mapper);
        tree.dump(state.getObfuscationMapping().wrap(dumper));
        index = dumper.getIndex();
    }
}
