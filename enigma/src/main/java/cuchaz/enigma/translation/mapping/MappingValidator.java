package cuchaz.enigma.translation.mapping;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.DefEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
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

	public void validateRename(ValidationContext vc, Entry<?> entry, String name) {
		Collection<Entry<?>> equivalentEntries = index.getEntryResolver().resolveEquivalentEntries(entry);
		for (Entry<?> equivalentEntry : equivalentEntries) {
			equivalentEntry.validateName(vc, name);
			validateUnique(vc, equivalentEntry, name);
		}
	}

	private void validateUnique(ValidationContext vc, Entry<?> entry, String name) {
		ClassEntry containingClass = entry.getContainingClass();
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		//Step 1, check it's unique within its own siblings
		Collection<Entry<?>> directTranslatedSiblings = obfToDeobf.getSiblings(entry).stream()
				.map(deobfuscator::translate)
				.collect(Collectors.toList());
		for (Entry<?> sibling : directTranslatedSiblings) {
			if (entry.canConflictWith(sibling) && sibling.getName().equals(name) && !isSynthetic(entry) && !isSynthetic(sibling)) {
				// allow clash if one is synthetic and the other is not
				Entry<?> parent = entry.getParent();
				if (parent != null) {
					vc.raise(Message.NONUNIQUE_NAME_CLASS, name, parent);
				} else {
					vc.raise(Message.NONUNIQUE_NAME, name);
				}
			}
		}

		//Step 2, check ancestors, ignoring members invisible to children
		Set<ClassEntry> ancestors = inheritanceIndex.getAncestors(containingClass);
		for (ClassEntry ancestor : ancestors) {
			Entry<?> reparentedEntry = entry.replaceAncestor(containingClass, ancestor);
			Entry<?> translatedEntry = Objects.requireNonNull(deobfuscator.translate(reparentedEntry), "Translation failed");
			Collection<Entry<?>> translatedSiblings = obfToDeobf.getSiblings(reparentedEntry).stream()
					.filter(it->!entry.equals(it))//e.g. for root classes, ensure we dont match the name against itself
					.filter(this::isVisibleToChildren)
					.collect(Collectors.toList());
			for (Entry<?> parentSibling : translatedSiblings) {
				Entry<?> parentSiblingTranslated = Objects.requireNonNull(deobfuscator.translate(parentSibling), "Translation failed");
				if (translatedEntry.canConflictWith(parentSiblingTranslated) && parentSiblingTranslated.getName().equals(name) && !isAcceptableOverride(parentSibling, entry)) {
					Entry<?> parent = translatedEntry.getParent();
					if (parent != null) {
						vc.raise(Message.NONUNIQUE_NAME_CLASS, name, parent);
					} else {
						vc.raise(Message.NONUNIQUE_NAME, name);
					}
				}
			}
		}

		//Step 3, if this entry is visible to children, see if it clashes with any of their names
		if (isVisibleToChildren(entry)) {
			Collection<ClassEntry> children = inheritanceIndex.getDescendants(containingClass);
			for (ClassEntry child : children) {
				Entry<?> reparentedEntry = entry.replaceAncestor(containingClass, child);
				Entry<?> translatedEntry = Objects.requireNonNull(deobfuscator.translate(reparentedEntry), "Translation failed");
				Collection<Entry<?>> siblings = obfToDeobf.getSiblings(reparentedEntry).stream()
						.filter(it->!entry.equals(it))//e.g. for root classes, ensure we dont match the name against itself
						.collect(Collectors.toList());
				for (Entry<?> childSibling : siblings) {
					Entry<?> childSiblingTranslated = Objects.requireNonNull(deobfuscator.translate(childSibling), "Translation failed");
					if (translatedEntry.canConflictWith(childSiblingTranslated) && childSiblingTranslated.getName().equals(name) && !isAcceptableOverride(entry, childSibling)) {
						Entry<?> parent = translatedEntry.getParent();
						if (parent != null) {
							vc.raise(Message.NONUNIQUE_NAME_CLASS, name, parent);
						} else {
							vc.raise(Message.NONUNIQUE_NAME, name);
						}
					}
				}
			}
		}
	}

	private boolean isVisibleToChildren(Entry<?> entry) {
		if (entry instanceof DefEntry) {
			return !((DefEntry<?>) entry).getAccess().isPrivate();
		}
		AccessFlags accessFlags = index.getEntryIndex().getEntryAccess(entry);
		if (accessFlags != null) {
			return !accessFlags.isPrivate();
		}
		return true;//unknown, assume yes
	}

	private boolean isAcceptableOverride(Entry<?> ancestor, Entry<?> descendent) {
		if (ancestor instanceof FieldEntry && descendent instanceof FieldEntry){
			return true;//fields don't apply here
		}

		AccessFlags ancestorFlags = findAccessFlags(ancestor);
		AccessFlags descendentFlags = findAccessFlags(descendent);

		if (ancestorFlags == null || descendentFlags == null) {
			return false;//we can't make any assumptions
		}

		//bad == accessLevel < superAccessLevel
		return !(descendentFlags.getAccessLevel() < ancestorFlags.getAccessLevel());
	}

	private boolean isSynthetic(Entry<?> entry) {
		AccessFlags accessFlags = findAccessFlags(entry);
		return accessFlags != null && accessFlags.isSynthetic();
	}

	private AccessFlags findAccessFlags(Entry<?> entry) {
		return (entry instanceof DefEntry) ? ((DefEntry<?>) entry).getAccess() : index.getEntryIndex()
				.getEntryAccess(entry);
	}

}
