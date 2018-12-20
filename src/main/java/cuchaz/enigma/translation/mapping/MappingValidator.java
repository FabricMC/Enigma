package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Collection;

public class MappingValidator {
	private final MappingTree<EntryMapping> obfToDeobf;
	private final MappingPropagator propagator;

	public MappingValidator(MappingTree<EntryMapping> obfToDeobf, MappingPropagator propagator) {
		this.obfToDeobf = obfToDeobf;
		this.propagator = propagator;
	}

	public void validateRename(Entry<?> entry, String name) throws IllegalNameException {
		validateRename(propagator.getPropagationTargets(entry), name);
	}

	public void validateRename(Collection<Entry<?>> targets, String name) throws IllegalNameException {
		for (Entry<?> entry : targets) {
			entry.validateName(name);
		}
		validateUnique(targets, name);
	}

	private void validateUnique(Collection<Entry<?>> targets, String name) {
		for (Entry<?> target : targets) {
			Collection<Entry<?>> siblings = obfToDeobf.getSiblings(target);
			if (!isUnique(target, siblings, name)) {
				throw new IllegalNameException(name, "Name is not unique in " + target + "!");
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
