/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation.representation.entry;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;

public abstract class ParentedEntry<P extends Entry<?>> implements Entry<P> {
	protected final P parent;
	protected final String name;
	protected final @Nullable String javadocs;

	protected ParentedEntry(P parent, String name, @Nullable String javadocs) {
		this.parent = parent;
		this.name = Objects.requireNonNull(name, "Name cannot be null");
		this.javadocs = javadocs;
	}

	@Override
	public abstract ParentedEntry<P> withParent(P parent);

	@Override
	public abstract ParentedEntry<P> withName(String name);

	protected abstract TranslateResult<? extends ParentedEntry<P>> extendedTranslate(Translator translator, @NotNull EntryMapping mapping);

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public String getFullName() {
		return this.parent.getFullName() + "." + this.name;
	}

	@Override
	public String getContextualName() {
		return this.parent.getContextualName() + "." + this.name;
	}

	@Override
	@Nullable
	public P getParent() {
		return parent;
	}

	@Nullable
	@Override
	public String getJavadocs() {
		return javadocs;
	}

	@Override
	public TranslateResult<? extends ParentedEntry<P>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		P parent = getParent();
		EntryMapping mapping = resolveMapping(resolver, mappings);

		if (parent == null) {
			return this.extendedTranslate(translator, mapping);
		}

		P translatedParent = translator.translate(parent);
		return this.withParent(translatedParent).extendedTranslate(translator, mapping);
	}

	private EntryMapping resolveMapping(EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		for (ParentedEntry<P> entry : resolver.resolveEntry(this, ResolutionStrategy.RESOLVE_ROOT)) {
			EntryMapping mapping = mappings.get(entry);

			if (mapping != null) {
				return mapping;
			}
		}

		return EntryMapping.DEFAULT;
	}
}
