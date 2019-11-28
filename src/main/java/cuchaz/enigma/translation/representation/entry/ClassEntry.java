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

import com.strobel.assembler.metadata.TypeReference;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.NameValidator;
import cuchaz.enigma.translation.representation.TypeDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ClassEntry extends ParentedEntry<ClassEntry> implements Comparable<ClassEntry> {
	private final String fullName;

	public ClassEntry(String className) {
		this(getOuterClass(className), getInnerName(className), null);
	}

	public ClassEntry(@Nullable ClassEntry parent, String className) {
		this(parent, className, null);
	}

	public ClassEntry(@Nullable ClassEntry parent, String className, @Nullable String javadocs) {
		super(parent, className, javadocs);
		if (parent != null) {
			fullName = parent.getFullName() + "$" + name;
		} else {
			fullName = name;
		}

		if (parent == null && className.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name must be in JVM format. ie, path/to/package/class$inner : " + className);
		}
	}

	public static ClassEntry parse(TypeReference typeReference) {
		return new ClassEntry(typeReference.getInternalName());
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getFullName() {
		return fullName;
	}

	@Override
	public ClassEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		if (name.charAt(0) == '[') {
			String translatedName = translator.translate(new TypeDescriptor(name)).toString();
			return new ClassEntry(parent, translatedName);
		}

		String translatedName = mapping != null ? mapping.getTargetName() : name;
		String docs = mapping != null ? mapping.getJavadoc() : null;
		return new ClassEntry(parent, translatedName, docs);
	}

	@Override
	public ClassEntry getContainingClass() {
		return this;
	}

	@Override
	public int hashCode() {
		return fullName.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassEntry && equals((ClassEntry) other);
	}

	public boolean equals(ClassEntry other) {
		return other != null && Objects.equals(parent, other.parent) && this.name.equals(other.name);
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		return true;
	}

	@Override
	public void validateName(String name) throws IllegalNameException {
		NameValidator.validateClassName(name, !isInnerClass());
	}

	@Override
	public ClassEntry withName(String name) {
		return new ClassEntry(parent, name, javadocs);
	}

	@Override
	public ClassEntry withParent(ClassEntry parent) {
		return new ClassEntry(parent, name, javadocs);
	}

	@Override
	public String toString() {
		return getFullName();
	}

	public String getPackageName() {
		return getPackageName(fullName);
	}

	public String getSimpleName() {
		int packagePos = name.lastIndexOf('/');
		if (packagePos > 0) {
			return name.substring(packagePos + 1);
		}
		return name;
	}

	public boolean isInnerClass() {
		return parent != null;
	}

	@Nullable
	public ClassEntry getOuterClass() {
		return parent;
	}

	@Nonnull
	public ClassEntry getOutermostClass() {
		if (parent == null) {
			return this;
		}
		return parent.getOutermostClass();
	}

	public ClassEntry buildClassEntry(List<ClassEntry> classChain) {
		assert (classChain.contains(this));
		StringBuilder buf = new StringBuilder();
		for (ClassEntry chainEntry : classChain) {
			if (buf.length() == 0) {
				buf.append(chainEntry.getFullName());
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

	public boolean isJre() {
		String packageName = getPackageName();
		return packageName != null && (packageName.startsWith("java") || packageName.startsWith("javax"));
	}

	public static String getPackageName(String name) {
		int pos = name.lastIndexOf('/');
		if (pos > 0) {
			return name.substring(0, pos);
		}
		return null;
	}

	@Nullable
	public static ClassEntry getOuterClass(String name) {
		int index = name.lastIndexOf('$');
		if (index >= 0) {
			return new ClassEntry(name.substring(0, index));
		}
		return null;
	}

	public static String getInnerName(String name) {
		int innerClassPos = name.lastIndexOf('$');
		if (innerClassPos > 0) {
			return name.substring(innerClassPos + 1);
		}
		return name;
	}

	@Override
	public String getSourceRemapName() {
		ClassEntry outerClass = getOuterClass();
		if (outerClass != null) {
			return outerClass.getSourceRemapName() + "." + name;
		}
		return getSimpleName();
	}

	@Override
	public int compareTo(ClassEntry entry) {
		String fullName = getFullName();
		String otherFullName = entry.getFullName();
		if (fullName.length() != otherFullName.length()) {
			return fullName.length() - otherFullName.length();
		}
		return fullName.compareTo(otherFullName);
	}
}
