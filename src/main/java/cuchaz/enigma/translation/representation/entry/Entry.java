/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.translation.representation.entry;

import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingSet;
import cuchaz.enigma.translation.mapping.NameValidator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface Entry extends Translatable {
	String getName();

	ClassEntry getContainingClass();

	default List<Entry> getAncestry() {
		List<Entry> entries = new ArrayList<>();
		entries.add(this);
		return entries;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	default <E extends Entry> E findAncestor(Class<E> type) {
		List<Entry> ancestry = getAncestry();
		for (int i = ancestry.size() - 1; i >= 0; i--) {
			Entry ancestor = ancestry.get(i);
			if (type.isAssignableFrom(ancestor.getClass())) {
				return (E) ancestor;
			}
		}
		return null;
	}

	default <E extends Entry> Entry replaceAncestor(E target, E replacement) {
		if (equals(target)) {
			return replacement;
		}
		return this;
	}

	@Override
	default Translatable translate(Translator translator, MappingSet<EntryMapping> mappings) {
		return translate(translator, mappings.getMapping(this));
	}

	Entry translate(Translator translator, @Nullable EntryMapping mapping);

	boolean shallowEquals(Entry entry);

	boolean canConflictWith(Entry entry);

	default void validateName(String name) throws IllegalNameException {
		NameValidator.validateIdentifier(name);
	}
}
