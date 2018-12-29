package cuchaz.enigma.analysis.index;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;

public class ReferenceIndex implements JarIndexer {
	private final Multimap<MethodEntry, MethodEntry> methodReferences = HashMultimap.create();
	private final Multimap<MethodEntry, EntryReference<MethodEntry, MethodDefEntry>> referencesToMethods = HashMultimap.create();
	private final Multimap<ClassEntry, EntryReference<ClassEntry, MethodDefEntry>> referencesToClasses = HashMultimap.create();

	private final Multimap<MethodEntry, FieldEntry> fieldReferences = HashMultimap.create();
	private final Multimap<FieldEntry, EntryReference<FieldEntry, MethodDefEntry>> referencesToFields = HashMultimap.create();

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry) {
		// TODO: should references be resolved? should they be resolved to original entry or just closest? (fields too)
		referencesToMethods.put(referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry));
		methodReferences.put(callerEntry, referencedEntry);

		if (referencedEntry.isConstructor()) {
			ClassEntry referencedClass = referencedEntry.getParent();
			referencesToClasses.put(referencedClass, new EntryReference<>(referencedClass, referencedEntry.getName(), callerEntry));
		}
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry) {
		fieldReferences.put(callerEntry, referencedEntry);
		referencesToFields.put(referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry));
	}

	public Collection<MethodEntry> getMethodsReferencedBy(MethodEntry entry) {
		return methodReferences.get(entry);
	}

	public Collection<FieldEntry> getFieldsReferencedBy(MethodEntry entry) {
		return fieldReferences.get(entry);
	}

	public Collection<EntryReference<FieldEntry, MethodDefEntry>> getReferencesToField(FieldEntry entry) {
		return referencesToFields.get(entry);
	}

	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getReferencesToClass(ClassEntry entry) {
		return referencesToClasses.get(entry);
	}

	public Collection<EntryReference<MethodEntry, MethodDefEntry>> getReferencesToMethod(MethodEntry entry) {
		return referencesToMethods.get(entry);
	}
}
