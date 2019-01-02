package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Collection;

public class MappingValidator {
	private final EntryTree<EntryMapping> obfToDeobf;
	private final EntryResolver entryResolver;

	public MappingValidator(EntryTree<EntryMapping> obfToDeobf, EntryResolver entryResolver) {
		this.obfToDeobf = obfToDeobf;
		this.entryResolver = entryResolver;
	}

	public void validateRename(Entry<?> entry, String name) throws IllegalNameException {
		Collection<Entry<?>> equivalentEntries = entryResolver.resolveEquivalentEntries(entry);
		for (Entry<?> e : equivalentEntries) {
			e.validateName(name);
		}
		validateUnique(equivalentEntries, name);
	}

	private void validateUnique(Collection<Entry<?>> hierarchy, String name) {
		for (Entry<?> entry : hierarchy) {
			Collection<Entry<?>> siblings = obfToDeobf.getSiblings(entry);
			if (!isUnique(entry, siblings, name)) {
				throw new IllegalNameException(name, "Name is not unique in " + entry + "!");
			}
		}
	}

	private boolean isUnique(Entry<?> entry, Collection<Entry<?>> siblings, String name) {
		for (Entry<?> child : siblings) {
			if (entry.canConflictWith(child) && child.getName().equals(name)) {
				return false;
			}
		}
		return true;
	}
}
