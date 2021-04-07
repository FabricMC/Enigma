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

import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.MethodDescriptor;

public class MethodEntry extends ParentedEntry<ClassEntry> implements Comparable<MethodEntry> {

	protected final MethodDescriptor descriptor;

	public MethodEntry(ClassEntry parent, String name, MethodDescriptor descriptor) {
		this(parent, name, descriptor, null);
	}

	public MethodEntry(ClassEntry parent, String name, MethodDescriptor descriptor, String javadocs) {
		super(parent, name, javadocs);

		Preconditions.checkNotNull(parent, "Parent cannot be null");
		Preconditions.checkNotNull(descriptor, "Method descriptor cannot be null");

		this.descriptor = descriptor;
	}

	public static MethodEntry parse(String owner, String name, String desc) {
		return new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc), null);
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
	protected TranslateResult<? extends MethodEntry> extendedTranslate(Translator translator, @Nullable EntryMapping mapping) {
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		String docs = mapping != null ? mapping.getJavadoc() : null;
		return TranslateResult.of(
				mapping == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new MethodEntry(parent, translatedName, translator.translate(descriptor), docs)
		);
	}

	@Override
	public MethodEntry withName(String name) {
		return new MethodEntry(parent, name, descriptor, javadocs);
	}

	@Override
	public MethodEntry withParent(ClassEntry parent) {
		return new MethodEntry(new ClassEntry(parent.getFullName()), name, descriptor, javadocs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.name, this.descriptor);
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
			return methodEntry.parent.equals(parent) && methodEntry.descriptor.canConflictWith(descriptor);
		}
		return false;
	}

	@Override
	public String toString() {
		return this.getFullName() + this.descriptor;
	}

	@Override
	public int compareTo(MethodEntry entry) {
		return (name + descriptor.toString()).compareTo(entry.name + entry.descriptor.toString());
	}
}
