package cuchaz.enigma.gui.stats;

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
import cuchaz.enigma.utils.I18n;

import java.util.*;

public class StatsGenerator {
    private final EntryIndex entryIndex;
    private final EntryRemapper mapper;
    private final EntryResolver entryResolver;
    private final List<ObfuscationTestService> obfuscationTestServices;
    private final List<NameProposalService> nameProposalServices;

    public StatsGenerator(EnigmaProject project) {
        entryIndex = project.getJarIndex().getEntryIndex();
        mapper = project.getMapper();
        entryResolver = project.getJarIndex().getEntryResolver();
        obfuscationTestServices = project.getEnigma().getServices().get(ObfuscationTestService.TYPE);
        nameProposalServices = project.getEnigma().getServices().get(NameProposalService.TYPE);
    }

    public StatsResult generate(ProgressListener progress, Set<StatsMember> includedMembers, String topLevelPackage, boolean includeSynthetic) {
        includedMembers = EnumSet.copyOf(includedMembers);
        int totalWork = 0;
        int totalMappable = 0;

        if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
            totalWork += entryIndex.getMethods().size();
        }

        if (includedMembers.contains(StatsMember.FIELDS)) {
            totalWork += entryIndex.getFields().size();
        }

        if (includedMembers.contains(StatsMember.CLASSES)) {
            totalWork += entryIndex.getClasses().size();
        }

        progress.init(totalWork, I18n.translate("progress.stats"));

        Map<String, Integer> counts = new HashMap<>();

        int numDone = 0;
        if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
            for (MethodEntry method : entryIndex.getMethods()) {
                progress.step(numDone++, I18n.translate("type.methods"));
                MethodEntry root = entryResolver
                        .resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
                        .stream()
                        .findFirst()
                        .orElseThrow(AssertionError::new);

                if (root == method && (!((MethodDefEntry) method).getAccess().isSynthetic() || includeSynthetic)) {
                    if (includedMembers.contains(StatsMember.METHODS)) {
                        update(counts, method);
                        totalMappable ++;
                    }

                    if (includedMembers.contains(StatsMember.PARAMETERS)) {
                        int index = ((MethodDefEntry) method).getAccess().isStatic() ? 0 : 1;
                        for (TypeDescriptor argument : method.getDesc().getArgumentDescs()) {
                            update(counts, new LocalVariableEntry(method, index, "", true,null));
                            index += argument.getSize();
                            totalMappable ++;
                        }
                    }
                }
            }
        }

        if (includedMembers.contains(StatsMember.FIELDS)) {
            for (FieldEntry field : entryIndex.getFields()) {
                progress.step(numDone++, I18n.translate("type.fields"));
                if (!((FieldDefEntry)field).getAccess().isSynthetic() || includeSynthetic) {
                    update(counts, field);
                    totalMappable ++;
                }
            }
        }

        if (includedMembers.contains(StatsMember.CLASSES)) {
            for (ClassEntry clazz : entryIndex.getClasses()) {
                progress.step(numDone++, I18n.translate("type.classes"));
                update(counts, clazz);
                totalMappable ++;
            }
        }

        progress.step(-1, I18n.translate("progress.stats.data"));

        StatsResult.Tree<Integer> tree = new StatsResult.Tree<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getKey().startsWith(topLevelPackage)) {
                tree.getNode(entry.getKey()).value = entry.getValue();
            }
        }

        tree.collapse(tree.root);
        return new StatsResult(totalMappable, counts.values().stream().mapToInt(i -> i).sum(), tree);
    }

    private void update(Map<String, Integer> counts, Entry<?> entry) {
        if (isObfuscated(entry)) {
            String parent = mapper.deobfuscate(entry.getAncestry().get(0)).getName().replace('/', '.');
            counts.put(parent, counts.getOrDefault(parent, 0) + 1);
        }
    }

    private boolean isObfuscated(Entry<?> entry) {
        String name = entry.getName();

        if (!obfuscationTestServices.isEmpty()) {
            for (ObfuscationTestService service : obfuscationTestServices) {
                if (service.testDeobfuscated(entry)) {
                    return false;
                }
            }
        }

        if (!nameProposalServices.isEmpty()) {
            for (NameProposalService service : nameProposalServices) {
                if (service.proposeName(entry, mapper).isPresent()) {
                    return false;
                }
            }
        }

        String mappedName = mapper.deobfuscate(entry).getName();
        if (mappedName != null && !mappedName.isEmpty() && !mappedName.equals(name)) {
            return false;
        }

        return true;
    }
}
