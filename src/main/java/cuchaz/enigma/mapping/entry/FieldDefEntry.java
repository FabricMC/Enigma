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
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.TypeDescriptor;

public class FieldDefEntry extends FieldEntry implements DefEntry {
	private final AccessFlags access;
	private final Signature signature;

	public FieldDefEntry(ClassEntry ownerEntry, String name, TypeDescriptor desc, Signature signature, AccessFlags access) {
		super(ownerEntry, name, desc);
		Preconditions.checkNotNull(access, "Field access cannot be null");
		Preconditions.checkNotNull(signature, "Field signature cannot be null");
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
	public FieldDefEntry updateOwnership(ClassEntry owner) {
		return new FieldDefEntry(owner, this.name, this.desc, signature, access);
	}
}
