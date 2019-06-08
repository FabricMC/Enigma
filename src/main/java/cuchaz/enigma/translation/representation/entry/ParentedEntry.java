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

import com.google.common.base.Preconditions;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;

import javax.annotation.Nullable;

public abstract class ParentedEntry<P extends Entry<?>> implements Entry<P> {
	protected final P parent;
	protected final String name;
	protected final @Nullable String javadocs;

	protected ParentedEntry(P parent, String name, String javadocs) {
		this.parent = parent;
		this.name = name;
		this.javadocs = javadocs;

		Preconditions.checkNotNull(name, "Name cannot be null");
	}

	@Override
	public abstract ParentedEntry<P> withParent(P parent);

	@Override
	public abstract ParentedEntry<P> withName(String name);

	protected abstract ParentedEntry<P> translate(Translator translator, @Nullable EntryMapping mapping);

	@Override
	public String getName() {
		return name;
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
	public ParentedEntry<P> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		P parent = getParent();
		EntryMapping mapping = resolveMapping(resolver, mappings);
		if (parent == null) {
			return translate(translator, mapping);
		}
		P translatedParent = translator.translate(parent);
		return withParent(translatedParent).translate(translator, mapping);
	}

	private EntryMapping resolveMapping(EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		for (ParentedEntry<P> entry : resolver.resolveEntry(this, ResolutionStrategy.RESOLVE_ROOT)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				return mapping;
			}
		}
		return null;
	}
}
