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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.mapping.IdentifierValidation;
import cuchaz.enigma.utils.validation.ValidationContext;

public interface Entry<P extends Entry<?>> extends Translatable {
	/**
	 * Returns the default name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's the same as {@link #getSimpleName()}.</p>
	 * <p>For other classes, it's the same as {@link #getFullName()}.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 *     <li>Outer class: "domain.name.ClassA"</li>
	 *     <li>Inner class: "ClassB"</li>
	 *     <li>Method: "methodC"</li>
	 * </ul>
	 */
	String getName();

	/**
	 * Returns the simple name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's the same as {@link #getName()}.</p>
	 * <p>For other classes, it's their name without the package name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 *     <li>Outer class: "ClassA"</li>
	 *     <li>Inner class: "ClassB"</li>
	 *     <li>Method: "methodC"</li>
	 * </ul>
	 */
	String getSimpleName();

	/**
	 * Returns the full name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's their name prefixed with the full name
	 * of their parent entry.</p>
	 * <p>For other classes, it's their name prefixed with their package name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 *     <li>Outer class: "domain.name.ClassA"</li>
	 *     <li>Inner class: "domain.name.ClassA$ClassB"</li>
	 *     <li>Method: "domain.name.ClassA.methodC"</li>
	 * </ul>
	 */
	String getFullName();

	/**
	 * Returns the contextual name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's their name prefixed with the contextual
	 * name of their parent entry.</p>
	 * <p>For other classes, it's only their simple name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 *     <li>Outer class: "ClassA"</li>
	 *     <li>Inner class: "ClassA$ClassB"</li>
	 *     <li>Method: "ClassA.methodC"</li>
	 * </ul>
	 */
	String getContextualName();

	String getJavadocs();

	default String getSourceRemapName() {
		return getName();
	}

	/**
	 * Returns the parent entry of this entry.
	 *
	 * <p>The parent entry should be a {@linkplain MethodEntry method} for local variables,
	 * a {@linkplain ClassEntry class} for methods, fields and inner classes, and {@code null}
	 * for other classes.</p>
	 */
	@Nullable
	P getParent();

	Class<P> getParentType();

	Entry<P> withName(String name);

	Entry<P> withParent(P parent);

	boolean canConflictWith(Entry<?> entry);

	@Nullable
	default ClassEntry getContainingClass() {
		P parent = getParent();
		if (parent == null) {
			return null;
		}
		if (parent instanceof ClassEntry) {
			return (ClassEntry) parent;
		}
		return parent.getContainingClass();
	}

	default List<Entry<?>> getAncestry() {
		P parent = getParent();
		List<Entry<?>> entries = new ArrayList<>();
		if (parent != null) {
			entries.addAll(parent.getAncestry());
		}
		entries.add(this);
		return entries;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	default <E extends Entry<?>> E findAncestor(Class<E> type) {
		List<Entry<?>> ancestry = getAncestry();
		for (int i = ancestry.size() - 1; i >= 0; i--) {
			Entry<?> ancestor = ancestry.get(i);
			if (type.isAssignableFrom(ancestor.getClass())) {
				return (E) ancestor;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	default <E extends Entry<?>> Entry<P> replaceAncestor(E target, E replacement) {
		if (replacement.equals(target)) {
			return this;
		}

		if (equals(target)) {
			return (Entry<P>) replacement;
		}

		P parent = getParent();
		if (parent == null) {
			return this;
		}

		return withParent((P) parent.replaceAncestor(target, replacement));
	}

	default void validateName(ValidationContext vc, String name) {
		IdentifierValidation.validateIdentifier(vc, name);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	default <C extends Entry<?>> Entry<C> castParent(Class<C> parentType) {
		if (parentType.equals(getParentType())) {
			return (Entry<C>) this;
		}
		return null;
	}
}
