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

import javax.annotation.Nullable;
import java.util.Objects;

public class FieldEntry extends ParentedEntry<ClassEntry> implements Comparable<FieldEntry> {
	protected final TypeDescriptor desc;

	public FieldEntry(ClassEntry parent, String name, TypeDescriptor desc) {
		this(parent, name, desc, null);
	}

	public FieldEntry(ClassEntry parent, String name, TypeDescriptor desc, String javadocs) {
		super(parent, name, javadocs);

		Preconditions.checkNotNull(parent, "Owner cannot be null");
		Preconditions.checkNotNull(desc, "Field descriptor cannot be null");

		this.desc = desc;
	}

	public static FieldEntry parse(String owner, String name, String desc) {
		return new FieldEntry(new ClassEntry(owner), name, new TypeDescriptor(desc), null);
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	public TypeDescriptor getDesc() {
		return this.desc;
	}

	@Override
	public FieldEntry withName(String name) {
		return new FieldEntry(parent, name, desc, null);
	}

	@Override
	public FieldEntry withParent(ClassEntry parent) {
		return new FieldEntry(parent, this.name, this.desc, null);
	}

	@Override
	protected FieldEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		String docs = mapping != null ? mapping.getJavadoc() : null;
		return new FieldEntry(parent, translatedName, translator.translate(desc), docs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.name, this.desc);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldEntry && equals((FieldEntry) other);
	}

	public boolean equals(FieldEntry other) {
		return this.parent.equals(other.parent) && name.equals(other.name) && desc.equals(other.desc);
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		return entry instanceof FieldEntry && ((FieldEntry) entry).parent.equals(parent);
	}

	@Override
	public String toString() {
		return this.parent.getFullName() + "." + this.name + ":" + this.desc;
	}

	@Override
	public int compareTo(FieldEntry entry) {
		return (name + desc.toString()).compareTo(entry.name + entry.desc.toString());
	}
}
