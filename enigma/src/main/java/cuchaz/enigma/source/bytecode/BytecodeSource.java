package cuchaz.enigma.source.bytecode;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BytecodeSource implements Source {
    private final ClassNode classNode;
    private final EntryRemapper remapper;

    public BytecodeSource(ClassNode classNode, EntryRemapper remapper) {
        this.classNode = classNode;
        this.remapper = remapper;
    }

    @Override
    public String asString() {
        return index().getSource();
    }

    @Override
    public Source withJavadocs(EntryRemapper remapper) {
        return new BytecodeSource(classNode, remapper);
    }

    @Override
    public SourceIndex index() {
        SourceIndex index = new SourceIndex();

        EngimaTextifier textifier = new EngimaTextifier(index);
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, textifier, writer);

        ClassNode node = this.classNode;

        if (remapper != null) {
            ClassNode translatedNode = new ClassNode();
            node.accept(new TranslationClassVisitor(remapper.getDeobfuscator(), Enigma.ASM_VERSION, translatedNode));
            node = translatedNode;
        }

        node.accept(traceClassVisitor);
        index.setSource(out.toString());

        return index;
    }
}
