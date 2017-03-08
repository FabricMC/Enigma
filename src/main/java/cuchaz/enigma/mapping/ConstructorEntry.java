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

import cuchaz.enigma.utils.Utils;

public class ConstructorEntry implements BehaviorEntry {

	private ClassEntry classEntry;
	private Signature signature;

	public ConstructorEntry(ClassEntry classEntry) {
		this(classEntry, null);
	}

	public ConstructorEntry(ClassEntry classEntry, Signature signature) {
		if (classEntry == null) {
			throw new IllegalArgumentException("Class cannot be null!");
		}

		this.classEntry = classEntry;
		this.signature = signature;
	}

	public ConstructorEntry(ConstructorEntry other, String newClassName) {
		this.classEntry = new ClassEntry(newClassName);
		this.signature = other.signature;
	}

	@Override
	public ClassEntry getClassEntry() {
		return this.classEntry;
	}

	@Override
	public String getName() {
		if (isStatic()) {
			return "<clinit>";
		}
		return "<init>";
	}

	public boolean isStatic() {
		return this.signature == null;
	}

	@Override
	public Signature getSignature() {
		return this.signature;
	}

	@Override
	public String getClassName() {
		return this.classEntry.getName();
	}

	@Override
	public ConstructorEntry cloneToNewClass(ClassEntry classEntry) {
		return new ConstructorEntry(this, classEntry.getName());
	}

	@Override
	public int hashCode() {
		if (isStatic()) {
			return Utils.combineHashesOrdered(this.classEntry);
		} else {
			return Utils.combineHashesOrdered(this.classEntry, this.signature);
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ConstructorEntry && equals((ConstructorEntry) other);
	}

	public boolean equals(ConstructorEntry other) {
		if (isStatic() != other.isStatic()) {
			return false;
		}

		if (isStatic()) {
			return this.classEntry.equals(other.classEntry);
		} else {
			return this.classEntry.equals(other.classEntry) && this.signature.equals(other.signature);
		}
	}

	@Override
	public String toString() {
		if (isStatic()) {
			return this.classEntry.getName() + "." + getName();
		} else {
			return this.classEntry.getName() + "." + getName() + this.signature;
		}
	}
}
