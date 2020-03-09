package cuchaz.enigma.source.procyon;

import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.procyon.index.SourceIndexVisitor;
import cuchaz.enigma.source.procyon.transformers.AddJavadocsAstTransform;
import cuchaz.enigma.translation.mapping.EntryRemapper;

import java.io.StringWriter;

public class ProcyonSource implements Source {
    private final DecompilerSettings settings;
    private final CompilationUnit tree;
    private String string;

    public ProcyonSource(CompilationUnit tree, DecompilerSettings settings) {
        this.settings = settings;
        this.tree = tree;
    }

    @Override
    public SourceIndex index() {
        SourceIndex index = new SourceIndex(asString());
        tree.acceptVisitor(new SourceIndexVisitor(), index);
        return index;
    }

    @Override
    public String asString() {
        if (string == null) {
            StringWriter writer = new StringWriter();
            tree.acceptVisitor(new JavaOutputVisitor(new PlainTextOutput(writer), settings), null);
            string = writer.toString();
        }

        return string;
    }

    @Override
    public Source addJavadocs(EntryRemapper remapper) {
        CompilationUnit remappedTree = (CompilationUnit) tree.clone();
        new AddJavadocsAstTransform(remapper).run(remappedTree);
        return new ProcyonSource(remappedTree, settings);
    }
}
