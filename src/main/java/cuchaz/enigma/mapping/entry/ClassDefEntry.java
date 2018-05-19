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

public class ClassDefEntry extends ClassEntry {
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

	public AccessFlags getAccess() {
		return access;
	}
}
