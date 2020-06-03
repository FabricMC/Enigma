package cuchaz.enigma.source.cfr;

import com.google.common.io.ByteStreams;
import cuchaz.enigma.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class CfrDecompiler implements Decompiler {
    private final DCCommonState state;

    public CfrDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
        Map<String, String> options = new HashMap<>();

        state = new DCCommonState(OptionsImpl.getFactory().create(options), new ClassFileSource2() {
            @Override
            public JarContent addJarContent(String s, AnalysisType analysisType) {
                return null;
            }

            @Override
            public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {

            }

            @Override
            public Collection<String> addJar(String jarPath) {
                return null;
            }

            @Override
            public String getPossiblyRenamedPath(String path) {
                return path;
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String path) {
                ClassNode node = classProvider.getClassNode(path.substring(0, path.lastIndexOf('.')));

                if (node == null) {
                    try (InputStream classResource = CfrDecompiler.class.getClassLoader().getResourceAsStream(path)) {
                        if (classResource != null) {
                            return new Pair<>(ByteStreams.toByteArray(classResource), path);
                        }
                    } catch (IOException ignored) {}

                    return null;
                }

                ClassWriter cw = new ClassWriter(0);
                node.accept(cw);
                return new Pair<>(cw.toByteArray(), path);
            }
        });
    }

    @Override
    public Source getSource(String className) {
        DCCommonState state = this.state;
        Options options = state.getOptions();

        ObfuscationMapping mapping = MappingFactory.get(options, state);
        state = new DCCommonState(state, mapping);
        ClassFile tree = state.getClassFileMaybePath(className);

        state.configureWith(tree);

        // To make sure we're analysing the cached version
        try {
            tree = state.getClassFile(tree.getClassType());
        } catch (CannotLoadClassException ignored) {}

        if (options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)) {
            tree.loadInnerClasses(state);
        }

        if (options.getOption(OptionsImpl.RENAME_DUP_MEMBERS)) {
            MemberNameResolver.resolveNames(state, ListFactory.newList(state.getClassCache().getLoadedTypes()));
        }

        TypeUsageCollectingDumper typeUsageCollector = new TypeUsageCollectingDumper(options, tree);
        tree.analyseTop(state, typeUsageCollector);
        return new CfrSource(tree, state, typeUsageCollector.getRealTypeUsageInformation());
    }
}
