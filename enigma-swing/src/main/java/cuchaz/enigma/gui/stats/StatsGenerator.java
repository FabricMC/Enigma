package cuchaz.enigma.gui.stats;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public class StatsGenerator {
	private final EnigmaProject project;
	private final EntryIndex entryIndex;
	private final EntryRemapper mapper;
	private final EntryResolver entryResolver;

	public StatsGenerator(EnigmaProject project) {
		this.project = project;
		this.entryIndex = project.getJarIndex().getEntryIndex();
		this.mapper = project.getMapper();
		this.entryResolver = project.getJarIndex().getEntryResolver();
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

		String topLevelPackageSlash = topLevelPackage.replace(".", "/");

		int numDone = 0;
		if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
			for (MethodEntry method : entryIndex.getMethods()) {
				progress.step(numDone++, I18n.translate("type.methods"));
				MethodEntry root = entryResolver
						.resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
						.stream()
						.findFirst()
						.orElseThrow(AssertionError::new);

				ClassEntry clazz = root.getParent();
				if (root == method && this.mapper.deobfuscate(clazz).getPackageName().startsWith(topLevelPackageSlash)) {
					if (includedMembers.contains(StatsMember.METHODS) && !((MethodDefEntry) method).getAccess().isSynthetic()) {
						update(counts, method);
						totalMappable++;
					}

					if (includedMembers.contains(StatsMember.PARAMETERS) && (!((MethodDefEntry) method).getAccess().isSynthetic() || includeSynthetic)) {
						int index = ((MethodDefEntry) method).getAccess().isStatic() ? 0 : 1;
						for (TypeDescriptor argument : method.getDesc().getArgumentDescs()) {
							update(counts, new LocalVariableEntry(method, index, "", true, null));
							index += argument.getSize();
							totalMappable++;
						}
					}
				}
			}
		}

		if (includedMembers.contains(StatsMember.FIELDS)) {
			for (FieldEntry field : entryIndex.getFields()) {
				progress.step(numDone++, I18n.translate("type.fields"));
				ClassEntry clazz = field.getParent();
				if (!((FieldDefEntry) field).getAccess().isSynthetic() && this.mapper.deobfuscate(clazz).getPackageName().startsWith(topLevelPackageSlash)) {
					update(counts, field);
					totalMappable++;
				}
			}
		}

		if (includedMembers.contains(StatsMember.CLASSES)) {
			for (ClassEntry clazz : entryIndex.getClasses()) {
				progress.step(numDone++, I18n.translate("type.classes"));
				if (this.mapper.deobfuscate(clazz).getPackageName().startsWith(topLevelPackageSlash)) {
					update(counts, clazz);
					totalMappable++;
				}
			}
		}

		if (includedMembers.contains(StatsMember.FIELDS)) {
			for (FieldEntry field : entryIndex.getFields()) {
				progress.step(numDone++, I18n.translate("type.fields"));

				if (!((FieldDefEntry) field).getAccess().isSynthetic()) {
					update(counts, field);
					totalMappable++;
				}
			}
		}

		if (includedMembers.contains(StatsMember.CLASSES)) {
			for (ClassEntry clazz : entryIndex.getClasses()) {
				progress.step(numDone++, I18n.translate("type.classes"));
				update(counts, clazz);
				totalMappable++;
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
		if (project.isObfuscated(entry)) {
			String parent = mapper.deobfuscate(entry.getAncestry().get(0)).getName().replace('/', '.');
			counts.put(parent, counts.getOrDefault(parent, 0) + 1);
		}
	}
}
