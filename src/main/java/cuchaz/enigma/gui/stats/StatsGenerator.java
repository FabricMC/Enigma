package cuchaz.enigma.gui.stats;

import com.google.gson.GsonBuilder;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;

import java.util.*;

public class StatsGenerator {
    private final EntryIndex entryIndex;
    private final EntryRemapper mapper;
    private final EntryResolver entryResolver;
    private final ObfuscationTestService obfuscationTestService;
    private final NameProposalService nameProposalService;

    public StatsGenerator(EnigmaProject project) {
        entryIndex = project.getJarIndex().getEntryIndex();
        mapper = project.getMapper();
        entryResolver = project.getJarIndex().getEntryResolver();
        obfuscationTestService = project.getEnigma().getServices().get(ObfuscationTestService.TYPE).orElse(null);
        nameProposalService = project.getEnigma().getServices().get(NameProposalService.TYPE).orElse(null);
    }

    public String generate(ProgressListener progress, Set<StatsMember> includedMembers) {
        includedMembers = EnumSet.copyOf(includedMembers);
        int totalWork = 0;

        if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
            totalWork += entryIndex.getMethods().size();
        }

        if (includedMembers.contains(StatsMember.FIELDS)) {
            totalWork += entryIndex.getFields().size();
        }

        if (includedMembers.contains(StatsMember.INNER_CLASSES)) {
            totalWork += entryIndex.getClasses().size();
        }

        progress.init(totalWork, "Generating stats");

        Map<String, Integer> counts = new HashMap<>();

        int numDone = 0;
        if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
            for (MethodEntry method : entryIndex.getMethods()) {
                progress.step(numDone++, "Methods");
                MethodEntry root = entryResolver
                        .resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
                        .stream()
                        .findFirst()
                        .orElseThrow(AssertionError::new);

                if (root == method && !((MethodDefEntry) method).getAccess().isSynthetic()) {
                    if (includedMembers.contains(StatsMember.METHODS)) {
                        update(counts, method);
                    }

                    if (includedMembers.contains(StatsMember.PARAMETERS)) {
                        int index = ((MethodDefEntry) method).getAccess().isStatic() ? 0 : 1;
                        for (TypeDescriptor argument : method.getDesc().getArgumentDescs()) {
                            update(counts, new LocalVariableEntry(method, index, "", true));
                            index += argument.getSize();
                        }
                    }
                }
            }
        }

        if (includedMembers.contains(StatsMember.FIELDS)) {
            for (FieldEntry field : entryIndex.getFields()) {
                progress.step(numDone++, "Fields");
                update(counts, field);
            }
        }

        if (includedMembers.contains(StatsMember.INNER_CLASSES)) {
            for (ClassEntry clazz : entryIndex.getClasses()) {
                progress.step(numDone++, "Classes");
                if (clazz.isInnerClass()) {
                    update(counts, clazz);
                }
            }
        }

        progress.step(-1, "Generating data");

        Tree<Integer> tree = new Tree<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getKey().startsWith("com.mojang")) continue; // just a few unmapped names, no point in having a subsection
            tree.getNode(entry.getKey()).value = entry.getValue();
        }

        tree.collapse(tree.root);
        return new GsonBuilder().setPrettyPrinting().create().toJson(tree.root);
    }

    private void update(Map<String, Integer> counts, Entry<?> entry) {
        if (isObfuscated(entry)) {
            String parent = mapper.deobfuscate(entry.getAncestry().get(0)).getName().replace('/', '.');
            counts.put(parent, counts.getOrDefault(parent, 0) + 1);
        }
    }

    private boolean isObfuscated(Entry<?> entry) {
        String name = entry.getName();

        if (obfuscationTestService != null && obfuscationTestService.testDeobfuscated(entry)) {
            return false;
        }

        if (nameProposalService != null && nameProposalService.proposeName(entry, mapper).isPresent()) {
            return false;
        }

        String mappedName = mapper.deobfuscate(entry).getName();
        if (mappedName != null && !mappedName.isEmpty() && !mappedName.equals(name)) {
            return false;
        }

        return true;
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
