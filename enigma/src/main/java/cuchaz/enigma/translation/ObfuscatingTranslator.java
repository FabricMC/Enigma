package cuchaz.enigma.translation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;

public class ObfuscatingTranslator implements Translator {
	private final JarIndex jarIndex;

	private final EntryTree<EntryMapping> inverseMappings = new HashEntryTree<>();
	private final EntryResolver resolver = new ObfuscatingResolver();

	public ObfuscatingTranslator(JarIndex jarIndex) {
		this.jarIndex = jarIndex;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <T extends Translatable> TranslateResult<T> extendedTranslate(@Nullable T translatable) {
		if (translatable == null) {
			return null;
		}

		if (translatable instanceof FieldEntry || translatable instanceof MethodEntry) {
			ParentedEntry<ClassEntry> key = obfOwnerAndDesc((ParentedEntry<ClassEntry>) translatable);
			EntryMapping mapping = inverseMappings.get(key);
			return mapping == null ? TranslateResult.obfuscated((T) key) : TranslateResult.deobfuscated((T) key.withName(mapping.targetName()));
		}

		return (TranslateResult<T>) translatable.extendedTranslate(this, resolver, inverseMappings);
	}

	public void refreshAll(Translator deobfuscator) {
		inverseMappings.clear();

		for (ClassEntry clazz : jarIndex.getEntryIndex().getClasses()) {
			inverseMappings.insert(deobfuscator.extendedTranslate(clazz).getValue(), new EntryMapping(clazz.getName()));
		}

		for (FieldEntry field : jarIndex.getEntryIndex().getFields()) {
			inverseMappings.insert(obfOwnerAndDesc(deobfuscator.extendedTranslate(field).getValue()), new EntryMapping(field.getName()));
		}

		for (MethodEntry method : jarIndex.getEntryIndex().getMethods()) {
			inverseMappings.insert(obfOwnerAndDesc(deobfuscator.extendedTranslate(method).getValue()), new EntryMapping(method.getName()));
		}
	}

	public void refreshName(Entry<?> entry, String oldDeobfName, String newDeobfName) {
		inverseMappings.remove(entry.withName(oldDeobfName));
		inverseMappings.insert(entry.withName(newDeobfName), new EntryMapping(entry.getName()));
	}

	@SuppressWarnings("unchecked")
	private <T extends ParentedEntry<ClassEntry>> T obfOwnerAndDesc(T translatable) {
		if (translatable.getParent() != null) {
			translatable = (T) translatable.withParent(extendedTranslate(translatable.getParent()).getValue());
		}

		if (translatable instanceof FieldEntry field) {
			translatable = (T) field.withDesc(extendedTranslate(field.getDesc()).getValue());
		} else if (translatable instanceof MethodEntry method) {
			translatable = (T) method.withDesc(extendedTranslate(method.getDesc()).getValue());
		}

		return translatable;
	}

	private class ObfuscatingResolver implements EntryResolver {
		@Override
		@SuppressWarnings("unchecked")
		public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
			ClassEntry containingClass;

			if (entry instanceof ClassEntry || (containingClass = entry.findAncestor(ClassEntry.class)) == null) {
				return List.of(entry);
			}

			List<E> result = new ArrayList<>();
			result.add(entry);

			for (ClassEntry parentClass : jarIndex.getInheritanceIndex().getAncestors(containingClass)) {
				result.add((E) entry.replaceAncestor(containingClass, parentClass));
			}

			return result;
		}

		@Override
		public Set<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
			return Set.of(entry);
		}

		@Override
		public Set<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
			return Set.of(methodEntry);
		}
	}
}
