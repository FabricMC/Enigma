package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Collection;
import java.util.stream.Collectors;

public class MappingValidator {
	private final EntryTree<EntryMapping> obfToDeobf;
	private final Translator deobfuscator;
	private final EntryResolver entryResolver;

	public MappingValidator(EntryTree<EntryMapping> obfToDeobf, Translator deobfuscator, EntryResolver entryResolver) {
		this.obfToDeobf = obfToDeobf;
		this.deobfuscator = deobfuscator;
		this.entryResolver = entryResolver;
	}

	public void validateRename(Entry<?> entry, String name) throws IllegalNameException {
		Collection<Entry<?>> equivalentEntries = entryResolver.resolveEquivalentEntries(entry);
		for (Entry<?> equivalentEntry : equivalentEntries) {
			equivalentEntry.validateName(name);
			validateUnique(equivalentEntry, name);
		}
	}

	private void validateUnique(Entry<?> entry, String name) {
		Entry<?> translatedEntry = deobfuscator.translate(entry);
		Collection<Entry<?>> translatedSiblings = obfToDeobf.getSiblings(entry).stream()
				.map(deobfuscator::translate)
				.collect(Collectors.toList());

		if (!isUnique(translatedEntry, translatedSiblings, name)) {
			throw new IllegalNameException(name, "Name is not unique in " + translatedEntry.getParent() + "!");
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
