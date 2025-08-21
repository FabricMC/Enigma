package cuchaz.enigma.analysis.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class PackageVisibilityIndex implements JarIndexer {
	private static boolean requiresSamePackage(AccessFlags entryAcc, EntryReference ref, InheritanceIndex inheritanceIndex) {
		if (entryAcc.isPublic()) {
			return false;
		}

		if (entryAcc.isProtected()) {
			ClassEntry contextClass = ref.context.getContainingClass();
			ClassEntry referencedClass = ref.entry.getContainingClass();

			if (!inheritanceIndex.getAncestors(contextClass).contains(referencedClass)) {
				return true; // access to protected member not in superclass
			}

			if (ref.targetType.getKind() == ReferenceTargetType.Kind.NONE) {
				return false; // access to superclass or static superclass member
			}

			// access to instance member only valid if target's class assignable to context class
			return !(ref.targetType.getKind() == ReferenceTargetType.Kind.UNINITIALIZED || ((ReferenceTargetType.ClassType) ref.targetType).getEntry().equals(contextClass) || inheritanceIndex.getAncestors(((ReferenceTargetType.ClassType) ref.targetType).getEntry()).contains(contextClass));
		}

		return true;
	}

	private final ConcurrentMap<ClassEntry, List<ClassEntry>> connections = new ConcurrentHashMap<>();
	private final List<Set<ClassEntry>> partitions = new ArrayList<>();
	private final Map<ClassEntry, Set<ClassEntry>> classPartitions = new HashMap<>();

	private void addConnection(ClassEntry classA, ClassEntry classB) {
		if (classA != classB) {
			JarIndex.synchronizedAdd(connections, classA, classB);
			JarIndex.synchronizedAdd(connections, classB, classA);
		}
	}

	private void buildPartition(Set<ClassEntry> unassignedClasses, Set<ClassEntry> partition, ClassEntry member) {
		List<ClassEntry> memberConnections = connections.get(member);

		if (memberConnections == null) {
			return;
		}

		for (ClassEntry connected : memberConnections) {
			if (unassignedClasses.remove(connected)) {
				partition.add(connected);
				buildPartition(unassignedClasses, partition, connected);
			}
		}
	}

	private void addConnections(EntryIndex entryIndex, ReferenceIndex referenceIndex, InheritanceIndex inheritanceIndex) {
		entryIndex.getFields().parallelStream().forEach(entry -> {
			AccessFlags entryAcc = entryIndex.getFieldAccess(entry);

			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<FieldEntry, MethodDefEntry> ref : referenceIndex.getReferencesToField(entry)) {
					if (requiresSamePackage(entryAcc, ref, inheritanceIndex)) {
						addConnection(ref.entry.getContainingClass(), ref.context.getContainingClass());
					}
				}
			}
		});

		entryIndex.getMethods().parallelStream().forEach(entry -> {
			AccessFlags entryAcc = entryIndex.getMethodAccess(entry);

			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<MethodEntry, MethodDefEntry> ref : referenceIndex.getReferencesToMethod(entry)) {
					if (requiresSamePackage(entryAcc, ref, inheritanceIndex)) {
						addConnection(ref.entry.getContainingClass(), ref.context.getContainingClass());
					}
				}
			}
		});

		entryIndex.getClasses().parallelStream().forEach(entry -> {
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
		});
	}

	private void addPartitions(EntryIndex entryIndex) {
		Set<ClassEntry> unassignedClasses = new HashSet<>(entryIndex.getClasses());

		while (!unassignedClasses.isEmpty()) {
			Iterator<ClassEntry> iterator = unassignedClasses.iterator();
			ClassEntry initialEntry = iterator.next();
			iterator.remove();

			HashSet<ClassEntry> partition = new HashSet<>();
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
