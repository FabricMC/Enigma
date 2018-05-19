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
import cuchaz.enigma.utils.Utils;

public class MethodEntry implements Entry {

	protected final ClassEntry classEntry;
	protected final String name;
	protected final MethodDescriptor descriptor;

	public MethodEntry(ClassEntry classEntry, String name, MethodDescriptor descriptor) {
		Preconditions.checkNotNull(classEntry, "Class cannot be null");
		Preconditions.checkNotNull(name, "Method name cannot be null");
		Preconditions.checkNotNull(descriptor, "Method descriptor cannot be null");

		this.classEntry = classEntry;
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public ClassEntry getOwnerClassEntry() {
		return this.classEntry;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public MethodDescriptor getDesc() {
		return this.descriptor;
	}

	public boolean isConstructor() {
		return name.equals("<init>") || name.equals("<clinit>");
	}

	@Override
	public String getClassName() {
		return this.classEntry.getName();
	}

	@Override
	public MethodEntry updateOwnership(ClassEntry classEntry) {
		return new MethodEntry(new ClassEntry(classEntry.getName()), name, descriptor);
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.classEntry, this.name, this.descriptor);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodEntry && equals((MethodEntry) other);
	}

	public boolean equals(MethodEntry other) {
		return this.classEntry.equals(other.getOwnerClassEntry()) && this.name.equals(other.getName()) && this.descriptor.equals(other.getDesc());
	}

	@Override
	public String toString() {
		return this.classEntry.getName() + "." + this.name + this.descriptor;
	}
}
