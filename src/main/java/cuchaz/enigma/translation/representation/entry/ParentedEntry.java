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

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class ParentedEntry<P extends Entry<?>> implements Entry<P> {
	protected final P parent;
	protected final String name;

	protected ParentedEntry(P parent, String name) {
		this.parent = parent;
		this.name = name;

		Preconditions.checkNotNull(name, "Name cannot be null");
	}

	@Override
	public abstract ParentedEntry<P> withParent(P parent);

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

	@Override
	public Translatable translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		P parent = getParent();
		EntryMapping mapping = resolveMapping(resolver, mappings);
		if (parent == null) {
			return translate(translator, mapping);
		}
		P translatedParent = translator.translate(parent);
		return withParent(translatedParent).translate(translator, mapping);
	}

	private EntryMapping resolveMapping(EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return resolver.resolveEntry(this).stream()
					.map(mappings::get).filter(Objects::nonNull)
					.findFirst().orElse(null);
	}
}
