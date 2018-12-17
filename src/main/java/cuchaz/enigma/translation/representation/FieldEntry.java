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

package cuchaz.enigma.translation.representation;

import com.google.common.base.Preconditions;
import cuchaz.enigma.utils.Utils;

public class FieldEntry implements Entry {

	protected final ClassEntry ownerEntry;
	protected final String name;
	protected final TypeDescriptor desc;

	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public FieldEntry(ClassEntry ownerEntry, String name, TypeDescriptor desc) {
		Preconditions.checkNotNull(ownerEntry, "Owner cannot be null");
		Preconditions.checkNotNull(name, "Field name cannot be null");
		Preconditions.checkNotNull(desc, "Field descriptor cannot be null");

		this.ownerEntry = ownerEntry;
		this.name = name;
		this.desc = desc;
	}

	@Override
	public ClassEntry getOwnerClassEntry() {
		return this.ownerEntry;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getClassName() {
		return this.ownerEntry.getName();
	}

	public TypeDescriptor getDesc() {
		return this.desc;
	}

	@Override
	public FieldEntry updateOwnership(ClassEntry owner) {
		return new FieldEntry(owner, this.name, this.desc);
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.ownerEntry, this.name, this.desc);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldEntry && equals((FieldEntry) other);
	}

	public boolean equals(FieldEntry other) {
		return this.ownerEntry.equals(other.ownerEntry) && this.name.equals(other.name) && this.desc.equals(other.desc);
	}

	@Override
	public String toString() {
		return this.ownerEntry.getName() + "." + this.name + ":" + this.desc;
	}
}
