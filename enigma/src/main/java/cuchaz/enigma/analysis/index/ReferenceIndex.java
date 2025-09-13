package cuchaz.enigma.analysis.index;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.api.view.entry.ClassEntryView;
import cuchaz.enigma.api.view.entry.EntryReferenceView;
import cuchaz.enigma.api.view.entry.FieldEntryView;
import cuchaz.enigma.api.view.entry.MethodEntryView;
import cuchaz.enigma.api.view.index.ReferenceIndexView;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class ReferenceIndex implements JarIndexer, ReferenceIndexView {
	private ConcurrentMap<MethodEntry, List<MethodEntry>> methodReferences = new ConcurrentHashMap<>();

	private ConcurrentMap<MethodEntry, List<EntryReference<MethodEntry, MethodDefEntry>>> referencesToMethods = new ConcurrentHashMap<>();
	private ConcurrentMap<ClassEntry, List<EntryReference<ClassEntry, MethodDefEntry>>> referencesToClasses = new ConcurrentHashMap<>();
	private ConcurrentMap<FieldEntry, List<EntryReference<FieldEntry, MethodDefEntry>>> referencesToFields = new ConcurrentHashMap<>();
	private ConcurrentMap<ClassEntry, List<EntryReference<ClassEntry, FieldDefEntry>>> fieldTypeReferences = new ConcurrentHashMap<>();
	private ConcurrentMap<ClassEntry, List<EntryReference<ClassEntry, MethodDefEntry>>> methodTypeReferences = new ConcurrentHashMap<>();

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		indexMethodDescriptor(methodEntry, methodEntry.getDesc());
	}

	private void indexMethodDescriptor(MethodDefEntry entry, MethodDescriptor descriptor) {
		for (TypeDescriptor typeDescriptor : descriptor.getArgumentDescs()) {
			indexMethodTypeDescriptor(entry, typeDescriptor);
		}

		indexMethodTypeDescriptor(entry, descriptor.getReturnDesc());
	}

	private void indexMethodTypeDescriptor(MethodDefEntry method, TypeDescriptor typeDescriptor) {
		if (typeDescriptor.isType()) {
			ClassEntry referencedClass = typeDescriptor.getTypeEntry();
			JarIndex.synchronizedAdd(methodTypeReferences, referencedClass, new EntryReference<>(referencedClass, referencedClass.getName(), method));
		} else if (typeDescriptor.isArray()) {
			indexMethodTypeDescriptor(method, typeDescriptor.getArrayType());
		}
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		indexFieldTypeDescriptor(fieldEntry, fieldEntry.getDesc());
	}

	private void indexFieldTypeDescriptor(FieldDefEntry field, TypeDescriptor typeDescriptor) {
		if (typeDescriptor.isType()) {
			ClassEntry referencedClass = typeDescriptor.getTypeEntry();
			JarIndex.synchronizedAdd(fieldTypeReferences, referencedClass, new EntryReference<>(referencedClass, referencedClass.getName(), field));
		} else if (typeDescriptor.isArray()) {
			indexFieldTypeDescriptor(field, typeDescriptor.getArrayType());
		}
	}

	@Override
	public void indexClassReference(MethodDefEntry callerEntry, ClassEntry referencedEntry, ReferenceTargetType targetType) {
		JarIndex.synchronizedAdd(referencesToClasses, referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry, targetType));
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
		JarIndex.synchronizedAdd(referencesToMethods, referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry, targetType));
		JarIndex.synchronizedAdd(methodReferences, callerEntry, referencedEntry);

		if (referencedEntry.isConstructor()) {
			ClassEntry referencedClass = referencedEntry.getParent();
			JarIndex.synchronizedAdd(referencesToClasses, referencedClass, new EntryReference<>(referencedClass, referencedEntry.getName(), callerEntry, targetType));
		}
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
		JarIndex.synchronizedAdd(referencesToFields, referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry, targetType));
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		if (lambda.getImplMethod() instanceof MethodEntry) {
			indexMethodReference(callerEntry, (MethodEntry) lambda.getImplMethod(), targetType);
		} else {
			indexFieldReference(callerEntry, (FieldEntry) lambda.getImplMethod(), targetType);
		}

		indexMethodDescriptor(callerEntry, lambda.getInvokedType());
		indexMethodDescriptor(callerEntry, lambda.getSamMethodType());
		indexMethodDescriptor(callerEntry, lambda.getInstantiatedMethodType());
	}

	@Override
	public void processIndex(JarIndex index) {
		methodReferences = remapReferences(index, methodReferences);
		referencesToMethods = remapReferencesTo(index, referencesToMethods);
		referencesToClasses = remapReferencesTo(index, referencesToClasses);
		referencesToFields = remapReferencesTo(index, referencesToFields);
		fieldTypeReferences = remapReferencesTo(index, fieldTypeReferences);
		methodTypeReferences = remapReferencesTo(index, methodTypeReferences);
	}

	private <K extends Entry<?>, V extends Entry<?>> ConcurrentMap<K, List<V>> remapReferences(JarIndex index, ConcurrentMap<K, List<V>> multimap) {
		ConcurrentMap<K, List<V>> resolved = new ConcurrentHashMap<>();

		multimap.entrySet().parallelStream().forEach(entry -> {
			for (V value : entry.getValue()) {
				JarIndex.synchronizedAdd(resolved, remap(index, entry.getKey()), remap(index, value));
			}
		});

		return resolved;
	}

	private <E extends Entry<?>, C extends Entry<?>> ConcurrentMap<E, List<EntryReference<E, C>>> remapReferencesTo(JarIndex index, ConcurrentMap<E, List<EntryReference<E, C>>> multimap) {
		ConcurrentMap<E, List<EntryReference<E, C>>> resolved = new ConcurrentHashMap<>();

		multimap.entrySet().parallelStream().forEach(entry -> {
			for (EntryReference<E, C> value : entry.getValue()) {
				JarIndex.synchronizedAdd(resolved, remap(index, entry.getKey()), remap(index, value));
			}
		});

		return resolved;
	}

	private <E extends Entry<?>> E remap(JarIndex index, E entry) {
		return index.getEntryResolver().resolveFirstEntry(entry, ResolutionStrategy.RESOLVE_CLOSEST);
	}

	private <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> remap(JarIndex index, EntryReference<E, C> reference) {
		return index.getEntryResolver().resolveFirstReference(reference, ResolutionStrategy.RESOLVE_CLOSEST);
	}

	public Collection<MethodEntry> getMethodsReferencedBy(MethodEntry entry) {
		return methodReferences.getOrDefault(entry, Collections.emptyList());
	}

	@Override
	public Collection<? extends MethodEntryView> getMethodsReferencedBy(MethodEntryView entry) {
		return getMethodsReferencedBy((MethodEntry) entry);
	}

	public Collection<EntryReference<FieldEntry, MethodDefEntry>> getReferencesToField(FieldEntry entry) {
		return referencesToFields.getOrDefault(entry, Collections.emptyList());
	}

	@Override
	public Collection<? extends EntryReferenceView> getReferencesToField(FieldEntryView entry) {
		return getReferencesToField((FieldEntry) entry);
	}

	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getReferencesToClass(ClassEntry entry) {
		return referencesToClasses.getOrDefault(entry, Collections.emptyList());
	}

	@Override
	public Collection<? extends EntryReferenceView> getReferencesToClass(ClassEntryView entry) {
		return getReferencesToClass((ClassEntry) entry);
	}

	public Collection<EntryReference<MethodEntry, MethodDefEntry>> getReferencesToMethod(MethodEntry entry) {
		return referencesToMethods.getOrDefault(entry, Collections.emptyList());
	}

	@Override
	public Collection<? extends EntryReferenceView> getReferencesToMethod(MethodEntryView entry) {
		return getReferencesToMethod((MethodEntry) entry);
	}

	public Collection<EntryReference<ClassEntry, FieldDefEntry>> getFieldTypeReferencesToClass(ClassEntry entry) {
		return fieldTypeReferences.getOrDefault(entry, Collections.emptyList());
	}

	@Override
	public Collection<? extends EntryReferenceView> getFieldTypeReferencesToClass(ClassEntryView entry) {
		return getFieldTypeReferencesToClass((ClassEntry) entry);
	}

	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getMethodTypeReferencesToClass(ClassEntry entry) {
		return methodTypeReferences.getOrDefault(entry, Collections.emptyList());
	}

	@Override
	public Collection<? extends EntryReferenceView> getMethodTypeReferencesToClass(ClassEntryView entry) {
		return getMethodTypeReferencesToClass((ClassEntry) entry);
	}
}
