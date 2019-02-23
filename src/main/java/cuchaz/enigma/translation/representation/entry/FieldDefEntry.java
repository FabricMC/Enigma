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
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.TypeDescriptor;

import javax.annotation.Nullable;

public class FieldDefEntry extends FieldEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public FieldDefEntry(ClassEntry owner, String name, TypeDescriptor desc, Signature signature, AccessFlags access) {
		super(owner, name, desc);
		Preconditions.checkNotNull(access, "Field access cannot be null");
		Preconditions.checkNotNull(signature, "Field signature cannot be null");
		this.access = access;
		this.signature = signature;
	}

	public static FieldDefEntry parse(ClassEntry owner, int access, String name, String desc, String signature) {
		return new FieldDefEntry(owner, name, new TypeDescriptor(desc), Signature.createTypedSignature(signature), new AccessFlags(access));
	}

	@Override
	public AccessFlags getAccess() {
		return access;
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	public FieldDefEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		TypeDescriptor translatedDesc = translator.translate(desc);
		Signature translatedSignature = translator.translate(signature);
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		AccessFlags translatedAccess = mapping != null ? mapping.getAccessModifier().transform(access) : access;
		return new FieldDefEntry(parent, translatedName, translatedDesc, translatedSignature, translatedAccess);
	}

	@Override
	public FieldDefEntry withName(String name) {
		return new FieldDefEntry(parent, name, desc, signature, access);
	}

	@Override
	public FieldDefEntry withParent(ClassEntry owner) {
		return new FieldDefEntry(owner, this.name, this.desc, signature, access);
	}
}
