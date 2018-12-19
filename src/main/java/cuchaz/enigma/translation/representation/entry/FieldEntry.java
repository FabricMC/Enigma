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
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FieldEntry implements ChildEntry<ClassEntry> {
	protected final ClassEntry parent;
	protected final String name;
	protected final TypeDescriptor desc;

	public FieldEntry(ClassEntry parent, String name, TypeDescriptor desc) {
		Preconditions.checkNotNull(parent, "Owner cannot be null");
		Preconditions.checkNotNull(name, "Field name cannot be null");
		Preconditions.checkNotNull(desc, "Field descriptor cannot be null");

		this.parent = parent;
		this.name = name;
		this.desc = desc;
	}

	@Nonnull
	@Override
	public ClassEntry getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public FieldEntry translateSelf(Translator translator, @Nullable EntryMapping mapping) {
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		return new FieldEntry(parent, translatedName, translator.translate(desc));
	}

	public TypeDescriptor getDesc() {
		return this.desc;
	}

	@Override
	public FieldEntry withParent(ClassEntry parent) {
		return new FieldEntry(parent, this.name, this.desc);
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.parent, this.name, this.desc);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldEntry && equals((FieldEntry) other);
	}

	public boolean equals(FieldEntry other) {
		return this.parent.equals(other.parent) && shallowEquals(other);
	}

	@Override
	public boolean shallowEquals(Entry entry) {
		if (entry instanceof FieldEntry) {
			FieldEntry fieldEntry = (FieldEntry) entry;
			return fieldEntry.name.equals(name) && fieldEntry.desc.equals(desc);
		}
		return false;
	}

	@Override
	public boolean canConflictWith(Entry entry) {
		return entry instanceof FieldEntry && ((FieldEntry) entry).parent.equals(parent);
	}

	@Override
	public String toString() {
		return this.parent.getName() + "." + this.name + ":" + this.desc;
	}
}
