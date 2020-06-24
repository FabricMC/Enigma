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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class EntryReference<E extends Entry<?>, C extends Entry<?>> implements Translatable {

	private static final List<String> CONSTRUCTOR_NON_NAMES = Arrays.asList("this", "super", "static");
	public E entry;
	public C context;
	public ReferenceTargetType targetType;

	private boolean sourceName;

	public EntryReference(E entry, String sourceName) {
		this(entry, sourceName, null);
	}

	public EntryReference(E entry, String sourceName, C context) {
		this(entry, sourceName, context, ReferenceTargetType.none());
	}

	public EntryReference(E entry, String sourceName, C context, ReferenceTargetType targetType) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}

		this.entry = entry;
		this.context = context;
		this.targetType = targetType;

		this.sourceName = sourceName != null && !sourceName.isEmpty();
		if (entry instanceof MethodEntry && ((MethodEntry) entry).isConstructor() && CONSTRUCTOR_NON_NAMES.contains(sourceName)) {
			this.sourceName = false;
		}
	}

	public EntryReference(E entry, C context, EntryReference<E, C> other) {
		this.entry = entry;
		this.context = context;
		this.sourceName = other.sourceName;
		this.targetType = other.targetType;
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

	public Entry<?> getNameableEntry() {
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
			return Objects.hash(entry.hashCode(), context.hashCode());
		}
		return entry.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EntryReference && equals((EntryReference<?, ?>) other);
	}

	public boolean equals(EntryReference<?, ?> other) {
		if (other == null) return false;

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

		if (targetType != null && targetType.getKind() != ReferenceTargetType.Kind.NONE) {
			buf.append(" on target of type ");
			buf.append(targetType);
		}

		return buf.toString();
	}

	@Override
	public TranslateResult<EntryReference<E, C>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return translator.extendedTranslate(this.entry).map(e -> new EntryReference<>(e, translator.translate(context), this));
	}

}
