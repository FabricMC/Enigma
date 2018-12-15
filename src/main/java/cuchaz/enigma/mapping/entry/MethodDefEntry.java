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

package cuchaz.enigma.mapping.entry;

import com.google.common.base.Preconditions;
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.Signature;

public class MethodDefEntry extends MethodEntry implements DefEntry {

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
	public MethodDefEntry updateOwnership(ClassEntry classEntry) {
		return new MethodDefEntry(new ClassEntry(classEntry.getName()), name, descriptor, signature, access);
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
