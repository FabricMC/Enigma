package cuchaz.enigma.analysis.index;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.ReferenceTargetType;
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

public class ReferenceIndex implements JarIndexer {
	private Multimap<MethodEntry, MethodEntry> methodReferences = HashMultimap.create();

	private Multimap<MethodEntry, EntryReference<MethodEntry, MethodDefEntry>> referencesToMethods = HashMultimap.create();
	private Multimap<ClassEntry, EntryReference<ClassEntry, MethodDefEntry>> referencesToClasses = HashMultimap.create();
	private Multimap<FieldEntry, EntryReference<FieldEntry, MethodDefEntry>> referencesToFields = HashMultimap.create();
	private Multimap<ClassEntry, EntryReference<ClassEntry, FieldDefEntry>> fieldTypeReferences = HashMultimap.create();
	private Multimap<ClassEntry, EntryReference<ClassEntry, MethodDefEntry>> methodTypeReferences = HashMultimap.create();

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
			methodTypeReferences.put(referencedClass, new EntryReference<>(referencedClass, referencedClass.getName(), method));
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
			fieldTypeReferences.put(referencedClass, new EntryReference<>(referencedClass, referencedClass.getName(), field));
		} else if (typeDescriptor.isArray()) {
			indexFieldTypeDescriptor(field, typeDescriptor.getArrayType());
		}
	}

	@Override
	public void indexClassReference(MethodDefEntry callerEntry, ClassEntry referencedEntry, ReferenceTargetType targetType) {
		referencesToClasses.put(referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry, targetType));
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
		referencesToMethods.put(referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry, targetType));
		methodReferences.put(callerEntry, referencedEntry);

		if (referencedEntry.isConstructor()) {
			ClassEntry referencedClass = referencedEntry.getParent();
			referencesToClasses.put(referencedClass, new EntryReference<>(referencedClass, referencedEntry.getName(), callerEntry, targetType));
		}
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
		referencesToFields.put(referencedEntry, new EntryReference<>(referencedEntry, referencedEntry.getName(), callerEntry, targetType));
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

	private <K extends Entry<?>, V extends Entry<?>> Multimap<K, V> remapReferences(JarIndex index, Multimap<K, V> multimap) {
		final int keySetSize = multimap.keySet().size();
		Multimap<K, V> resolved = HashMultimap.create(multimap.keySet().size(), keySetSize == 0 ? 0 : multimap.size() / keySetSize);

		for (Map.Entry<K, V> entry : multimap.entries()) {
			resolved.put(remap(index, entry.getKey()), remap(index, entry.getValue()));
		}

		return resolved;
	}

	private <E extends Entry<?>, C extends Entry<?>> Multimap<E, EntryReference<E, C>> remapReferencesTo(JarIndex index, Multimap<E, EntryReference<E, C>> multimap) {
		final int keySetSize = multimap.keySet().size();
		Multimap<E, EntryReference<E, C>> resolved = HashMultimap.create(keySetSize, keySetSize == 0 ? 0 : multimap.size() / keySetSize);

		for (Map.Entry<E, EntryReference<E, C>> entry : multimap.entries()) {
			resolved.put(remap(index, entry.getKey()), remap(index, entry.getValue()));
		}

		return resolved;
	}

	private <E extends Entry<?>> E remap(JarIndex index, E entry) {
		return index.getEntryResolver().resolveFirstEntry(entry, ResolutionStrategy.RESOLVE_CLOSEST);
	}

	private <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> remap(JarIndex index, EntryReference<E, C> reference) {
		return index.getEntryResolver().resolveFirstReference(reference, ResolutionStrategy.RESOLVE_CLOSEST);
	}

	public Collection<MethodEntry> getMethodsReferencedBy(MethodEntry entry) {
		return methodReferences.get(entry);
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

	public Collection<EntryReference<ClassEntry, FieldDefEntry>> getFieldTypeReferencesToClass(ClassEntry entry) {
		return fieldTypeReferences.get(entry);
	}

	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getMethodTypeReferencesToClass(ClassEntry entry) {
		return methodTypeReferences.get(entry);
	}
}
