package cuchaz.enigma.analysis.index;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.*;

import java.util.*;

public class PackageVisibilityIndex implements JarIndexer {
	private static boolean requiresSamePackage(AccessFlags entryAcc, EntryReference ref, InheritanceIndex inheritanceIndex) {
		if (entryAcc.isPublic()) return false;
		if (entryAcc.isProtected()) {
			Set<ClassEntry> callerAncestors = inheritanceIndex.getAncestors(ref.context.getContainingClass());
			return !callerAncestors.contains(ref.entry.getContainingClass());
		}
		return !entryAcc.isPrivate(); // if isPrivate is false, it must be package-private
	}

	private final HashMultimap<ClassEntry, ClassEntry> connections = HashMultimap.create();
	private final List<Set<ClassEntry>> partitions = Lists.newArrayList();
	private final Map<ClassEntry, Set<ClassEntry>> classPartitions = Maps.newHashMap();

	private void addConnection(ClassEntry classA, ClassEntry classB) {
		connections.put(classA, classB);
		connections.put(classB, classA);
	}

	private void buildPartition(Set<ClassEntry> unassignedClasses, Set<ClassEntry> partition, ClassEntry member) {
		for (ClassEntry connected : connections.get(member)) {
			if (unassignedClasses.remove(connected)) {
				partition.add(connected);
				buildPartition(unassignedClasses, partition, connected);
			}
		}
	}

	private void addConnections(EntryIndex entryIndex, ReferenceIndex referenceIndex, InheritanceIndex inheritanceIndex) {
		for (FieldEntry entry : entryIndex.getFields()) {
			AccessFlags entryAcc = entryIndex.getFieldAccess(entry);
			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<FieldEntry, MethodDefEntry> ref : referenceIndex.getReferencesToField(entry)) {
					if (requiresSamePackage(entryAcc, ref, inheritanceIndex)) {
						addConnection(ref.entry.getContainingClass(), ref.context.getContainingClass());
					}
				}
			}
		}

		for (MethodEntry entry : entryIndex.getMethods()) {
			AccessFlags entryAcc = entryIndex.getMethodAccess(entry);
			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<MethodEntry, MethodDefEntry> ref : referenceIndex.getReferencesToMethod(entry)) {
					if (requiresSamePackage(entryAcc, ref, inheritanceIndex)) {
						addConnection(ref.entry.getContainingClass(), ref.context.getContainingClass());
					}
				}
			}
		}

		for (ClassEntry entry : entryIndex.getClasses()) {
			AccessFlags entryAcc = entryIndex.getClassAccess(entry);
			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<ClassEntry, FieldDefEntry> ref : referenceIndex.getFieldTypeReferencesToClass(entry)) {
					if (requiresSamePackage(entryAcc, ref, inheritanceIndex)) {
						addConnection(ref.entry.getContainingClass(), ref.context.getContainingClass());
					}
				}

				for (EntryReference<ClassEntry, MethodDefEntry> ref : referenceIndex.getMethodTypeReferencesToClass(entry)) {
					if (requiresSamePackage(entryAcc, ref, inheritanceIndex)) {
						addConnection(ref.entry.getContainingClass(), ref.context.getContainingClass());
					}
				}
			}

			for (ClassEntry parent : inheritanceIndex.getParents(entry)) {
				AccessFlags parentAcc = entryIndex.getClassAccess(parent);
				if (parentAcc != null && !parentAcc.isPublic() && !parentAcc.isPrivate()) {
					addConnection(entry, parent);
				}
			}

			ClassEntry outerClass = entry.getOuterClass();
			if (outerClass != null) {
				addConnection(entry, outerClass);
			}
		}
	}

	private void addPartitions(EntryIndex entryIndex) {
		Set<ClassEntry> unassignedClasses = Sets.newHashSet(entryIndex.getClasses());
		while (!unassignedClasses.isEmpty()) {
			Iterator<ClassEntry> iterator = unassignedClasses.iterator();
			ClassEntry initialEntry = iterator.next();
			iterator.remove();

			HashSet<ClassEntry> partition = Sets.newHashSet();
			partition.add(initialEntry);
			buildPartition(unassignedClasses, partition, initialEntry);
			partitions.add(partition);
			for (ClassEntry entry : partition) {
				classPartitions.put(entry, partition);
			}
		}
	}

	public Collection<Set<ClassEntry>> getPartitions() {
		return partitions;
	}

	public Set<ClassEntry> getPartition(ClassEntry classEntry) {
		return classPartitions.get(classEntry);
	}

	@Override
	public void processIndex(JarIndex index) {
		EntryIndex entryIndex = index.getEntryIndex();
		ReferenceIndex referenceIndex = index.getReferenceIndex();
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();
		addConnections(entryIndex, referenceIndex, inheritanceIndex);
		addPartitions(entryIndex);
	}
}
