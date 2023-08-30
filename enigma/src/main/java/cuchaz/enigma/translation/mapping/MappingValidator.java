package cuchaz.enigma.translation.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class MappingValidator {
	private final EntryTree<EntryMapping> obfToDeobf;
	private final Translator deobfuscator;
	private final JarIndex index;

	public MappingValidator(EntryTree<EntryMapping> obfToDeobf, Translator deobfuscator, JarIndex index) {
		this.obfToDeobf = obfToDeobf;
		this.deobfuscator = deobfuscator;
		this.index = index;
	}

	public boolean validateRename(ValidationContext vc, Entry<?> entry, String name) {
		Collection<Entry<?>> equivalentEntries = index.getEntryResolver().resolveEquivalentEntries(entry);
		boolean error = false;

		for (Entry<?> equivalentEntry : equivalentEntries) {
			equivalentEntry.validateName(vc, name);
			error |= validateUnique(vc, equivalentEntry, name);
		}

		return error;
	}

	private boolean validateUnique(ValidationContext vc, Entry<?> entry, String name) {
		ClassEntry containingClass = entry.getContainingClass();
		Collection<ClassEntry> relatedClasses = getRelatedClasses(containingClass);

		boolean error = false;
		Entry<?> shadowedEntry;

		for (ClassEntry relatedClass : relatedClasses) {
			if (isStatic(entry) && relatedClass != containingClass) {
				// static entries can only conflict with entries in the same class
				continue;
			}

			Entry<?> relatedEntry = entry.replaceAncestor(containingClass, relatedClass);
			Entry<?> translatedEntry = deobfuscator.translate(relatedEntry);

			List<? extends Entry<?>> translatedSiblings = obfToDeobf.getSiblings(relatedEntry).stream()
					.filter(sibling -> !sibling.equals(entry)) // Don't check against yourself
					.map(deobfuscator::translate)
					.toList();

			if (!isUnique(translatedEntry, translatedSiblings, name)) {
				Entry<?> parent = translatedEntry.getParent();

				if (parent != null) {
					vc.raise(Message.NONUNIQUE_NAME_CLASS, name, parent);
				} else {
					vc.raise(Message.NONUNIQUE_NAME, name);
				}

				error = true;
			} else if ((shadowedEntry = getShadowedEntry(translatedEntry, translatedSiblings, name)) != null) {
				Entry<?> parent = shadowedEntry.getParent();

				if (parent != null) {
					vc.raise(Message.SHADOWED_NAME_CLASS, name, parent);
				} else {
					vc.raise(Message.SHADOWED_NAME, name);
				}
			}
		}

		return error;
	}

	private Collection<ClassEntry> getRelatedClasses(ClassEntry classEntry) {
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		Collection<ClassEntry> relatedClasses = new HashSet<>();
		relatedClasses.add(classEntry);
		relatedClasses.addAll(inheritanceIndex.getChildren(classEntry));
		relatedClasses.addAll(inheritanceIndex.getAncestors(classEntry));

		return relatedClasses;
	}

	private boolean isUnique(Entry<?> entry, List<? extends Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (canConflict(entry, sibling) && sibling.getName().equals(name)) {
				return false;
			}
		}

		return true;
	}

	private boolean canConflict(Entry<?> entry, Entry<?> sibling) {
		return entry.canConflictWith(sibling);
	}

	@Nullable
	private Entry<?> getShadowedEntry(Entry<?> entry, List<? extends Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (canShadow(entry, sibling) && sibling.getName().equals(name)) {
				return sibling;
			}
		}

		return null;
	}

	private boolean canShadow(Entry<?> entry, Entry<?> sibling) {
		return entry.canShadow(sibling);
	}

	private boolean isStatic(Entry<?> entry) {
		AccessFlags accessFlags = index.getEntryIndex().getEntryAccess(entry);
		return accessFlags != null && accessFlags.isStatic();
	}
}
