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

public class ClassDefEntry extends ClassEntry implements DefEntry {
	private final AccessFlags access;
	private final Signature signature;

	public ClassDefEntry(String className, Signature signature, AccessFlags access) {
		super(className);
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
		String mappedName = mapping != null ? mapping.getTargetName() : name;
		AccessFlags translatedAccess = mapping != null ? mapping.getAccessModifier().transform(access) : access;
		if (isInnerClass()) {
			ClassEntry outerClass = translator.translate(getOuterClass());
			return new ClassDefEntry(outerClass.name + "$" + mappedName, translatedSignature, translatedAccess);
		}
		return new ClassDefEntry(mappedName, translatedSignature, translatedAccess);
	}
}
