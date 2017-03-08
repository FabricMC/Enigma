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

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.utils.Utils;

import java.util.Arrays;
import java.util.List;

public class EntryReference<E extends Entry, C extends Entry> {

	private static final List<String> ConstructorNonNames = Arrays.asList("this", "super", "static");
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
		if (entry instanceof ConstructorEntry && ConstructorNonNames.contains(sourceName)) {
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
			return context.getClassEntry();
		}
		return entry.getClassEntry();
	}

	public boolean isNamed() {
		return this.sourceName;
	}

	public Entry getNameableEntry() {
		if (entry instanceof ConstructorEntry) {
			// renaming a constructor really means renaming the class
			return entry.getClassEntry();
		}
		return entry;
	}

	public String getNamableName() {
		if (getNameableEntry() instanceof ClassEntry) {
			ClassEntry classEntry = (ClassEntry) getNameableEntry();
			if (classEntry.isInnerClass()) {
				// make sure we only rename the inner class name
				return classEntry.getInnermostClassName();
			}
		}

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
}
