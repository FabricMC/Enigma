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

package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingSet;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;

import java.util.Arrays;
import java.util.List;

public class EntryReference<E extends Entry, C extends Entry> implements Translatable {

	private static final List<String> CONSTRUCTOR_NON_NAMES = Arrays.asList("this", "super", "static");
	public E entry;
	public C context;

	private boolean sourceName;

	public EntryReference(E entry, String sourceName) {
		this(entry, sourceName, null);
	}

	public EntryReference(E entry, String sourceName, C context) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}

		this.entry = entry;
		this.context = context;

		this.sourceName = sourceName != null && !sourceName.isEmpty();
		if (entry instanceof MethodEntry && ((MethodEntry) entry).isConstructor() && CONSTRUCTOR_NON_NAMES.contains(sourceName)) {
			this.sourceName = false;
		}
	}

	public EntryReference(E entry, C context, EntryReference<E, C> other) {
		this.entry = entry;
		this.context = context;
		this.sourceName = other.sourceName;
	}

	public ClassEntry getLocationClassEntry() {
		if (context != null) {
			return context.getContainingClass();
		}
		return entry.getContainingClass();
	}

	public boolean isNamed() {
		return this.sourceName;
	}

	public Entry getNameableEntry() {
		if (entry instanceof MethodEntry && ((MethodEntry) entry).isConstructor()) {
			// renaming a constructor really means renaming the class
			return entry.getContainingClass();
		}
		return entry;
	}

	public String getNameableName() {
		return getNameableEntry().getName();
	}

	@Override
	public int hashCode() {
		if (context != null) {
			return Utils.combineHashesOrdered(entry.hashCode(), context.hashCode());
		}
		return entry.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EntryReference && equals((EntryReference<?, ?>) other);
	}

	public boolean equals(EntryReference<?, ?> other) {
		// check entry first
		boolean isEntrySame = entry.equals(other.entry);
		if (!isEntrySame) {
			return false;
		}

		// check caller
		if (context == null && other.context == null) {
			return true;
		} else if (context != null && other.context != null) {
			return context.equals(other.context);
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(entry);
		if (context != null) {
			buf.append(" called from ");
			buf.append(context);
		}
		return buf.toString();
	}

	@Override
	public Translatable translate(Translator translator, MappingSet<EntryMapping> mappings) {
		return new EntryReference<>(translator.translate(entry), translator.translate(context), this);
	}
}
