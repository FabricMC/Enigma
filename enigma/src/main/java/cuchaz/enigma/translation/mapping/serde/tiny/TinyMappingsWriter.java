package cuchaz.enigma.translation.mapping.serde.tiny;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class TinyMappingsWriter implements MappingsWriter {
    private static final String VERSION_CONSTANT = "v1";
    private static final Joiner TAB_JOINER = Joiner.on('\t');

    //Possibly add a gui or a way to select the namespaces when exporting from the gui
    public static final TinyMappingsWriter INSTANCE = new TinyMappingsWriter("intermediary", "named");

    // HACK: as of enigma 0.13.1, some fields seem to appear duplicated?
    private final Set<String> writtenLines = new HashSet<>();
    private final String nameObf;
    private final String nameDeobf;

    public TinyMappingsWriter(String nameObf, String nameDeobf) {
        this.nameObf = nameObf;
        this.nameDeobf = nameDeobf;
    }

    @Override
    public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
        try {
            Files.deleteIfExists(path);
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writeLine(writer, new String[]{VERSION_CONSTANT, nameObf, nameDeobf});

            Lists.newArrayList(mappings).stream()
                    .map(EntryTreeNode::getEntry).sorted(Comparator.comparing(Object::toString))
                    .forEach(entry -> writeEntry(writer, mappings, entry));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeEntry(Writer writer, EntryTree<EntryMapping> mappings, Entry<?> entry) {
        EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
        if (node == null) {
            return;
        }

        Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);

        EntryMapping mapping = mappings.get(entry);

        // Do not write mappings without deobfuscated name since tiny v1 doesn't
        // support comments anyway
		if (mapping != null && mapping.targetName() != null) {
            if (entry instanceof ClassEntry) {
                writeClass(writer, (ClassEntry) entry, translator);
            } else if (entry instanceof FieldEntry) {
				writeLine(writer, serializeEntry(entry, mapping.targetName()));
            } else if (entry instanceof MethodEntry) {
				writeLine(writer, serializeEntry(entry, mapping.targetName()));
            }
        }

        writeChildren(writer, mappings, node);
    }

    private void writeChildren(Writer writer, EntryTree<EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
        node.getChildren().stream()
                .filter(e -> e instanceof FieldEntry).sorted()
                .forEach(child -> writeEntry(writer, mappings, child));

        node.getChildren().stream()
                .filter(e -> e instanceof MethodEntry).sorted()
                .forEach(child -> writeEntry(writer, mappings, child));

        node.getChildren().stream()
                .filter(e -> e instanceof ClassEntry).sorted()
                .forEach(child -> writeEntry(writer, mappings, child));
    }

    private void writeClass(Writer writer, ClassEntry entry, Translator translator) {
        ClassEntry translatedEntry = translator.translate(entry);

        String obfClassName = entry.getFullName();
        String deobfClassName = translatedEntry.getFullName();
        writeLine(writer, new String[]{"CLASS", obfClassName, deobfClassName});
    }

    private void writeLine(Writer writer, String[] data) {
        try {
            String line = TAB_JOINER.join(data) + "\n";
            if (writtenLines.add(line)) {
                writer.write(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] serializeEntry(Entry<?> entry, String... extraFields) {
        String[] data = null;

        if (entry instanceof FieldEntry) {
            data = new String[4 + extraFields.length];
            data[0] = "FIELD";
            data[1] = entry.getContainingClass().getFullName();
            data[2] = ((FieldEntry) entry).getDesc().toString();
            data[3] = entry.getName();
        } else if (entry instanceof MethodEntry) {
            data = new String[4 + extraFields.length];
            data[0] = "METHOD";
            data[1] = entry.getContainingClass().getFullName();
            data[2] = ((MethodEntry) entry).getDesc().toString();
            data[3] = entry.getName();
        } else if (entry instanceof ClassEntry) {
            data = new String[2 + extraFields.length];
            data[0] = "CLASS";
            data[1] = ((ClassEntry) entry).getFullName();
        }

        if (data != null) {
            System.arraycopy(extraFields, 0, data, data.length - extraFields.length, extraFields.length);
        }

        return data;
    }
}
