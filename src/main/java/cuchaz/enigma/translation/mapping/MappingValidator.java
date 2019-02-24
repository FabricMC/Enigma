package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class MappingValidator {
	private final EntryTree<EntryMapping> obfToDeobf;
	private final Translator deobfuscator;
	private final JarIndex index;

	public MappingValidator(EntryTree<EntryMapping> obfToDeobf, Translator deobfuscator, JarIndex index) {
		this.obfToDeobf = obfToDeobf;
		this.deobfuscator = deobfuscator;
		this.index = index;
	}

	public void validateRename(Entry<?> entry, String name) throws IllegalNameException {
		Collection<Entry<?>> equivalentEntries = index.getEntryResolver().resolveEquivalentEntries(entry);
		for (Entry<?> equivalentEntry : equivalentEntries) {
			equivalentEntry.validateName(name);
			validateUnique(equivalentEntry, name);
		}
	}

	private void validateUnique(Entry<?> entry, String name) {
		ClassEntry containingClass = entry.getContainingClass();
		Collection<ClassEntry> relatedClasses = getRelatedClasses(containingClass);

		for (ClassEntry relatedClass : relatedClasses) {
			Entry<?> relatedEntry = entry.replaceAncestor(containingClass, relatedClass);
			Entry<?> translatedEntry = deobfuscator.translate(relatedEntry);

			Collection<Entry<?>> translatedSiblings = obfToDeobf.getChildren(relatedClass).stream()
					.filter(e -> !entry.equals(e))
					.map(deobfuscator::translate)
					.collect(Collectors.toList());

			if (!isUnique(translatedEntry, translatedSiblings, name)) {
				throw new IllegalNameException(name, "Name is not unique in " + translatedEntry.getParent() + "!");
			}
		}
	}

	private Collection<ClassEntry> getRelatedClasses(ClassEntry classEntry) {
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		Collection<ClassEntry> relatedClasses = new HashSet<>();
		relatedClasses.add(classEntry);
		relatedClasses.addAll(inheritanceIndex.getChildren(classEntry));
		relatedClasses.addAll(inheritanceIndex.getAncestors(classEntry));

		return relatedClasses;
	}

	private boolean isUnique(Entry<?> entry, Collection<Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (entry.canConflictWith(sibling) && sibling.getName().equals(name)) {
				return false;
			}
		}
		return true;
	}
}
