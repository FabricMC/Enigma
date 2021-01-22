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
	public final E entry;
	public final C context;
	public final ReferenceTargetType targetType;
	private final boolean declaration; // if the ref goes to the decl of the item. when true context == null
	private final boolean sourceName;

	public static <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> declaration(E entry, String sourceName) {
		return new EntryReference<>(entry, sourceName, null, ReferenceTargetType.none(), true);
	}

	public EntryReference(E entry, String sourceName) {
		this(entry, sourceName, null);
	}

	public EntryReference(E entry, String sourceName, C context) {
		this(entry, sourceName, context, ReferenceTargetType.none());
	}

	public EntryReference(E entry, String sourceName, C context, ReferenceTargetType targetType) {
		this(entry, sourceName, context, targetType, false);
	}

	protected EntryReference(E entry, String sourceName, C context, ReferenceTargetType targetType, boolean declaration) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}

		this.entry = entry;
		this.context = context;
		this.targetType = targetType;
		this.declaration = declaration;

		this.sourceName = sourceName != null && !sourceName.isEmpty() &&
				!(entry instanceof MethodEntry && ((MethodEntry) entry).isConstructor() && CONSTRUCTOR_NON_NAMES.contains(sourceName));
	}

	public EntryReference(E entry, C context, EntryReference<E, C> other) {
		this.entry = entry;
		this.context = context;
		this.sourceName = other.sourceName;
		this.targetType = other.targetType;
		this.declaration = other.declaration;
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

	/**
	 * Returns whether this refers to the declaration of an entry.
	 */
	public boolean isDeclaration() {
		return this.declaration;
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
		return entry.hashCode() ^ Boolean.hashCode(this.declaration);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EntryReference && equals((EntryReference<?, ?>) other);
	}

	public boolean equals(EntryReference<?, ?> other) {
		return other != null
				&& Objects.equals(entry, other.entry)
				&& Objects.equals(context, other.context)
				&& declaration == other.declaration;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(entry);

		if (declaration) {
			buf.append("'s declaration");
			return buf.toString();
		}

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
