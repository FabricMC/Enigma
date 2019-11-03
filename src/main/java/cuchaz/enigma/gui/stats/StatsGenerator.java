package cuchaz.enigma.gui.stats;

import com.google.gson.GsonBuilder;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsGenerator {
    private final EntryIndex entryIndex;
    private final EntryRemapper mapper;
    private final EntryResolver entryResolver;
    private final ObfuscationTestService obfuscationTestService;

    public StatsGenerator(EnigmaProject project) {
        entryIndex = project.getJarIndex().getEntryIndex();
        mapper = project.getMapper();
        entryResolver = project.getJarIndex().getEntryResolver();
        obfuscationTestService = project.getEnigma().getServices().get(ObfuscationTestService.TYPE).orElse(null);
    }

    public String generate(StatsType type, ProgressListener progress) {
        int total = 0;
        if (type.includesMethods) total += entryIndex.getMethods().size();
        if (type.includesFields) total += entryIndex.getFields().size();

        progress.init(total, "Generating stats");

        Map<String, Integer> counts = new HashMap<>();

        int numDone = 0;
        if (type.includesMethods) {
            progress.step(numDone++, "Methods");
            for (MethodEntry method : entryIndex.getMethods()) {
                MethodEntry root = entryResolver
                        .resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
                        .stream()
                        .findFirst()
                        .orElseThrow(AssertionError::new);

                if (root == method && !((MethodDefEntry) method).getAccess().isSynthetic()) {
                    update(counts, method);
                }
            }
        }

        if (type.includesFields) {
            progress.step(numDone++, "Fields");
            for (FieldEntry field : entryIndex.getFields()) {
                update(counts, field);
            }
        }

        progress.step(numDone++, "Generating data");

        Tree<Integer> tree = new Tree<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getKey().startsWith("com.mojang")) continue; // just a few unmapped names, no point in having a subsection
            tree.getNode(entry.getKey()).value = entry.getValue();
        }

        tree.collapse(tree.root);
        return new GsonBuilder().setPrettyPrinting().create().toJson(tree.root);
    }

    private void update(Map<String, Integer> counts, Entry<ClassEntry> entry) {
        String obfName = entry.getName();
        String deobfName = mapper.deobfuscate(entry).getName();
        boolean obfuscated = obfName.equals(deobfName) && (obfuscationTestService == null || !obfuscationTestService.testDeobfuscated(entry));

        if (obfuscated) {
            String parent = mapper.deobfuscate(entry.getAncestry().get(0)).getName().replace('/', '.');
            counts.put(parent, counts.getOrDefault(parent, 0) + 1);
        }
    }

    private static class Tree<T> {
        public final Node<T> root;
        private final Map<String, Node<T>> nodes = new HashMap<>();

        public static class Node<T> {
            public String name;
            public T value;
            public List<Node<T>> children = new ArrayList<>();
            private final transient Map<String, Node<T>> namedChildren = new HashMap<>();

            public Node(String name, T value) {
                this.name = name;
                this.value = value;
            }
        }

        public Tree() {
            root = new Node<>("", null);
        }

        public Node<T> getNode(String name) {
            Node<T> node = nodes.get(name);

            if (node == null) {
                node = root;

                for (String part : name.split("\\.")) {
                    Node<T> child = node.namedChildren.get(part);

                    if (child == null) {
                        child = new Node<>(part, null);
                        node.namedChildren.put(part, child);
                        node.children.add(child);
                    }

                    node = child;
                }

                nodes.put(name, node);
            }

            return node;
        }

        public void collapse(Node<T> node) {
            while (node.children.size() == 1) {
                Node<T> child = node.children.get(0);
                //noinspection StringConcatenationInLoop
                node.name = node.name + "." + child.name;
                node.children = child.children;
                node.value = child.value;
            }

            for (Node<T> child : node.children) {
                collapse(child);
            }
        }
    }
}
