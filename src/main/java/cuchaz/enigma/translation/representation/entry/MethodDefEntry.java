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
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;

import javax.annotation.Nullable;

public class MethodDefEntry extends MethodEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public MethodDefEntry(ClassEntry classEntry, String name, MethodDescriptor descriptor, Signature signature, AccessFlags access) {
		super(classEntry, name, descriptor);
		Preconditions.checkNotNull(access, "Method access cannot be null");
		Preconditions.checkNotNull(signature, "Method signature cannot be null");
		this.access = access;
		this.signature = signature;
	}

	@Override
	public AccessFlags getAccess() {
		return access;
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	public MethodDefEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		MethodDescriptor translatedDesc = translator.translate(descriptor);
		Signature translatedSignature = translator.translate(signature);
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		AccessFlags translatedAccess = mapping != null ? mapping.getAccessModifier().transform(access) : access;
		return new MethodDefEntry(parent, translatedName, translatedDesc, translatedSignature, translatedAccess);
	}

	@Override
	public MethodDefEntry withParent(ClassEntry parent) {
		return new MethodDefEntry(new ClassEntry(parent.getFullName()), name, descriptor, signature, access);
	}

	public int getArgumentIndex(ClassDefEntry ownerEntry, int localVariableIndex) {
		int argumentIndex = localVariableIndex;

		// Enum constructors have an implicit "name" and "ordinal" parameter as well as "this"
		if (ownerEntry.getAccess().isEnum() && getName().startsWith("<")) {
			argumentIndex -= 2;
		}

		// If we're not static, "this" is bound to index 0
		if (!getAccess().isStatic()) {
			argumentIndex -= 1;
		}

		return argumentIndex;
	}
}
