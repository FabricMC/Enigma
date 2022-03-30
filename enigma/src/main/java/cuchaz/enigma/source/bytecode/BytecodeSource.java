package cuchaz.enigma.source.bytecode;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BytecodeSource implements Source {
    private final ClassNode classNode;

    public BytecodeSource(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public String asString() {
        return index().getSource();
    }

    @Override
    public Source withJavadocs(EntryRemapper remapper) {
        // TODO
        return this;
    }

    @Override
    public SourceIndex index() {
        SourceIndex index = new SourceIndex();

        EngimaTextifier textifier = new EngimaTextifier(index);
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, textifier, writer);
        classNode.accept(traceClassVisitor);

        index.setSource(out.toString());

        return index;
    }
}
