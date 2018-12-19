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
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.NameValidator;

import javax.annotation.Nullable;
import java.util.List;

// TODO: Warnings in log relating to bad inner classes
public class ClassEntry implements Entry {
	protected final String name;
	protected final boolean innerClass;

	public ClassEntry(String className) {
		Preconditions.checkNotNull(className, "Class name cannot be null");
		if (className.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name must be in JVM format. ie, path/to/package/class$inner : " + className);
		}

		this.name = className;
		this.innerClass = name.indexOf('$') >= 0;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getFullName() {
		return this.name;
	}

	@Override
	public ClassEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		if (mapping == null) {
			return this;
		}
		if (isInnerClass()) {
			ClassEntry outerClass = translator.translate(getOuterClass());
			return new ClassEntry(outerClass.name + "$" + mapping.getTargetName());
		}
		return new ClassEntry(mapping.getTargetName());
	}

	@Override
	public ClassEntry getContainingClass() {
		return this;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassEntry && equals((ClassEntry) other);
	}

	public boolean equals(ClassEntry other) {
		return other != null && this.name.equals(other.name);
	}

	@Override
	public boolean shallowEquals(Entry entry) {
		return entry instanceof ClassEntry && ((ClassEntry) entry).name.equals(name);
	}

	@Override
	public boolean canConflictWith(Entry entry) {
		return true;
	}

	@Override
	public void validateName(String name) throws IllegalNameException {
		NameValidator.validateClassName(name, true);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public String getPackageName() {
		return getPackageName(this.name);
	}

	public String getSimpleName() {
		int packagePos = name.lastIndexOf('/');
		int innerClassPos = name.lastIndexOf('$');
		if (packagePos > 0) {
			return name.substring(Math.max(packagePos, innerClassPos) + 1);
		}
		return name;
	}

	public boolean isInnerClass() {
		return innerClass;
	}

	@Nullable
	public ClassEntry getOuterClass() {
		if (!innerClass) {
			return null;
		}
		return new ClassEntry(name.substring(0, name.lastIndexOf('$')));
	}

	public ClassEntry buildClassEntry(List<ClassEntry> classChain) {
		assert (classChain.contains(this));
		StringBuilder buf = new StringBuilder();
		for (ClassEntry chainEntry : classChain) {
			if (buf.length() == 0) {
				buf.append(chainEntry.getName());
			} else {
				buf.append("$");
				buf.append(chainEntry.getSimpleName());
			}

			if (chainEntry == this) {
				break;
			}
		}
		return new ClassEntry(buf.toString());
	}

	public static String getPackageName(String name) {
		int pos = name.lastIndexOf('/');
		if (pos > 0) {
			return name.substring(0, pos);
		}
		return null;
	}
}
