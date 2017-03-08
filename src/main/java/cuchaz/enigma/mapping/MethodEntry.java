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

public class MethodEntry implements BehaviorEntry {

	private ClassEntry classEntry;
	private String name;
	private Signature signature;

	public MethodEntry(ClassEntry classEntry, String name, Signature signature) {
		if (classEntry == null) {
			throw new IllegalArgumentException("Class cannot be null!");
		}
		if (name == null) {
			throw new IllegalArgumentException("Method name cannot be null!");
		}
		if (signature == null) {
			throw new IllegalArgumentException("Method signature cannot be null!");
		}
		if (name.startsWith("<")) {
			throw new IllegalArgumentException("Don't use MethodEntry for a constructor!");
		}

		this.classEntry = classEntry;
		this.name = name;
		this.signature = signature;
	}

	public MethodEntry(MethodEntry other, String newClassName) {
		this.classEntry = new ClassEntry(newClassName);
		this.name = other.name;
		this.signature = other.signature;
	}

	@Override
	public ClassEntry getClassEntry() {
		return this.classEntry;
	}

	@Override
	public String getName() {
		return this.name;
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
	public MethodEntry cloneToNewClass(ClassEntry classEntry) {
		return new MethodEntry(this, classEntry.getName());
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.classEntry, this.name, this.signature);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodEntry && equals((MethodEntry) other);
	}

	public boolean equals(MethodEntry other) {
		return this.classEntry.equals(other.classEntry) && this.name.equals(other.name) && this.signature.equals(other.signature);
	}

	@Override
	public String toString() {
		return this.classEntry.getName() + "." + this.name + this.signature;
	}
}
