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

import cuchaz.enigma.mapping.entry.ClassEntry;
import cuchaz.enigma.mapping.entry.FieldEntry;
import cuchaz.enigma.throwables.IllegalNameException;

public class FieldMapping implements Comparable<FieldMapping>, MemberMapping<FieldEntry> {

	private String obfName;
	private String deobfName;
	private TypeDescriptor obfDesc;
	private Mappings.EntryModifier modifier;

	public FieldMapping(String obfName, TypeDescriptor obfDesc, String deobfName, Mappings.EntryModifier modifier) {
		this.obfName = obfName;
		this.deobfName = NameValidator.validateFieldName(deobfName);
		this.obfDesc = obfDesc;
		this.modifier = modifier;
	}

	@Override
	public FieldEntry getObfEntry(ClassEntry classEntry) {
		return new FieldEntry(classEntry, this.obfName, this.obfDesc);
	}

	@Override
	public String getObfName() {
		return this.obfName;
	}

	public void setObfName(String name) {
		try {
			NameValidator.validateMethodName(name);
		} catch (IllegalNameException ex) {
			// Invalid name, damn obfuscation! Map to a deob name with another name to avoid issues
			if (this.deobfName == null) {
				System.err.println("WARNING: " + name + " is conflicting, auto deobfuscate to " + (name + "_auto_deob"));
				setDeobfName(name + "_auto_deob");
			}
		}
		this.obfName = name;
	}

	public String getDeobfName() {
		return this.deobfName;
	}

	public void setDeobfName(String val) {
		this.deobfName = NameValidator.validateFieldName(val);
	}

	public TypeDescriptor getObfDesc() {
		return this.obfDesc;
	}

	public void setObfDesc(TypeDescriptor val) {
		this.obfDesc = val;
	}

	public Mappings.EntryModifier getModifier() {
		return modifier;
	}

	public void setModifier(Mappings.EntryModifier modifier) {
		this.modifier = modifier;
	}

	@Override
	public int compareTo(FieldMapping other) {
		return (this.obfName + this.obfDesc).compareTo(other.obfName + other.obfDesc);
	}

	public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {
		// rename obf classes in the desc
		TypeDescriptor newDesc = this.obfDesc.remap(className -> {
			if (className.equals(oldObfClassName)) {
				return newObfClassName;
			}
			return className;
		});

		if (!newDesc.equals(this.obfDesc)) {
			this.obfDesc = newDesc;
			return true;
		}
		return false;
	}

}
