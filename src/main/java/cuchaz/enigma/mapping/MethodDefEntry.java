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

package cuchaz.enigma.mapping;

import com.google.common.base.Preconditions;
import cuchaz.enigma.bytecode.AccessFlags;

public class MethodDefEntry extends MethodEntry {

	private final AccessFlags access;

	public MethodDefEntry(ClassEntry classEntry, String name, MethodDescriptor descriptor, AccessFlags access) {
		super(classEntry, name, descriptor);
		Preconditions.checkNotNull(access, "Method access cannot be null");
		this.access = access;
	}

	public AccessFlags getAccess() {
		return access;
	}

	@Override
	public MethodDefEntry updateOwnership(ClassEntry classEntry) {
		return new MethodDefEntry(new ClassEntry(classEntry.getName()), name, descriptor, access);
	}
}
