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

public class ArgumentEntry implements Entry {

	private BehaviorEntry behaviorEntry;
	private int index;
	private String name;

	public ArgumentEntry(BehaviorEntry behaviorEntry, int index, String name) {
		if (behaviorEntry == null) {
			throw new IllegalArgumentException("Behavior cannot be null!");
		}
		if (index < 0) {
			throw new IllegalArgumentException("Index must be non-negative!");
		}
		if (name == null) {
			throw new IllegalArgumentException("Argument name cannot be null!");
		}

		this.behaviorEntry = behaviorEntry;
		this.index = index;
		this.name = name;
	}

	public ArgumentEntry(ArgumentEntry other) {
		this.behaviorEntry = other.getBehaviorEntry();
		this.index = other.index;
		this.name = other.name;
	}

	public ArgumentEntry(ArgumentEntry other, String newClassName) {
		this.behaviorEntry = (BehaviorEntry) other.behaviorEntry.cloneToNewClass(new ClassEntry(newClassName));
		this.index = other.index;
		this.name = other.name;
	}

	public ArgumentEntry(ArgumentEntry other, BehaviorEntry entry) {
		this.behaviorEntry = entry;
		this.index = other.index;
		this.name = other.name;
	}

	public BehaviorEntry getBehaviorEntry() {
		return this.behaviorEntry;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public ClassEntry getClassEntry() {
		return this.behaviorEntry.getClassEntry();
	}

	@Override
	public String getClassName() {
		return this.behaviorEntry.getClassName();
	}

	@Override
	public ArgumentEntry cloneToNewClass(ClassEntry classEntry) {
		return new ArgumentEntry(this, classEntry.getName());
	}

	public String getMethodName() {
		return this.behaviorEntry.getName();
	}

	public Signature getMethodSignature() {
		return this.behaviorEntry.getSignature();
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.behaviorEntry, Integer.valueOf(this.index).hashCode(), this.name.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ArgumentEntry && equals((ArgumentEntry) other);
	}

	public boolean equals(ArgumentEntry other) {
		return this.behaviorEntry.equals(other.behaviorEntry) && this.index == other.index && this.name.equals(other.name);
	}

	@Override
	public String toString() {
		return this.behaviorEntry + "(" + this.index + ":" + this.name + ")";
	}
}
