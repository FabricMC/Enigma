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
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.utils.Utils;

import javax.annotation.Nullable;

public class MethodEntry extends ParentedEntry<ClassEntry> implements Comparable<MethodEntry> {

	protected final MethodDescriptor descriptor;

	public MethodEntry(ClassEntry parent, String name, MethodDescriptor descriptor) {
		super(parent, name);

		Preconditions.checkNotNull(parent, "Parent cannot be null");
		Preconditions.checkNotNull(descriptor, "Method descriptor cannot be null");

		this.descriptor = descriptor;
	}

	public static MethodEntry parse(String owner, String name, String desc) {
		return new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc));
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	public MethodDescriptor getDesc() {
		return this.descriptor;
	}

	public boolean isConstructor() {
		return name.equals("<init>") || name.equals("<clinit>");
	}

	@Override
	public MethodEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		return new MethodEntry(parent, translatedName, translator.translate(descriptor));
	}

	@Override
	public MethodEntry withParent(ClassEntry parent) {
		return new MethodEntry(new ClassEntry(parent.getFullName()), name, descriptor);
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.parent, this.name, this.descriptor);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodEntry && equals((MethodEntry) other);
	}

	public boolean equals(MethodEntry other) {
		return this.parent.equals(other.getParent()) && this.name.equals(other.getName()) && this.descriptor.equals(other.getDesc());
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		if (entry instanceof MethodEntry) {
			MethodEntry methodEntry = (MethodEntry) entry;
			return methodEntry.parent.equals(parent) && methodEntry.descriptor.equals(descriptor);
		}
		return false;
	}

	@Override
	public String toString() {
		return this.parent.getFullName() + "." + this.name + this.descriptor;
	}

	@Override
	public int compareTo(MethodEntry entry) {
		return (name + descriptor.toString()).compareTo(entry.name + entry.descriptor.toString());
	}
}
