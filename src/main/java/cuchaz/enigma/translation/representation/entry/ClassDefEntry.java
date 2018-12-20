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

import javax.annotation.Nullable;

public class ClassDefEntry extends ClassEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public ClassDefEntry(String className, Signature signature, AccessFlags access) {
		this(getOuterClass(className), getInnerName(className), signature, access);
	}

	public ClassDefEntry(ClassEntry parent, String className, Signature signature, AccessFlags access) {
		super(parent, className);
		Preconditions.checkNotNull(signature, "Class signature cannot be null");
		Preconditions.checkNotNull(access, "Class access cannot be null");
		this.signature = signature;
		this.access = access;
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	public AccessFlags getAccess() {
		return access;
	}

	@Override
	public ClassDefEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		Signature translatedSignature = translator.translate(signature);
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		AccessFlags translatedAccess = mapping != null ? mapping.getAccessModifier().transform(access) : access;
		return new ClassDefEntry(parent, translatedName, translatedSignature, translatedAccess);
	}

	@Override
	public ClassDefEntry withParent(ClassEntry parent) {
		return new ClassDefEntry(parent, name, signature, access);
	}
}
