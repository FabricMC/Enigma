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

package cuchaz.enigma.translation.mapping;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.ClassEntry;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.MethodEntry;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.throwables.MappingConflict;

import java.util.Map;

public class MethodMapping implements Comparable<MethodMapping>, MemberMapping<MethodEntry> {

	private String obfName;
	private String deobfName;
	private MethodDescriptor obfDescriptor;
	private Map<Integer, LocalVariableMapping> localVariables;
	private Mappings.EntryModifier modifier;

	public MethodMapping(String obfName, MethodDescriptor obfDescriptor) {
		this(obfName, obfDescriptor, null, Mappings.EntryModifier.UNCHANGED);
	}

	public MethodMapping(String obfName, MethodDescriptor obfDescriptor, String deobfName) {
		this(obfName, obfDescriptor, deobfName, Mappings.EntryModifier.UNCHANGED);
	}

	public MethodMapping(String obfName, MethodDescriptor obfDescriptor, String deobfName, Mappings.EntryModifier modifier) {
		Preconditions.checkNotNull(obfName, "Method obf name cannot be null");
		Preconditions.checkNotNull(obfDescriptor, "Method obf desc cannot be null");
		this.obfName = obfName;
		this.deobfName = NameValidator.validateMethodName(deobfName);
		this.obfDescriptor = obfDescriptor;
		this.localVariables = Maps.newTreeMap();
		this.modifier = modifier;
	}

	public MethodMapping(MethodMapping other, Translator translator) {
		this.obfName = other.obfName;
		this.deobfName = other.deobfName;
		this.modifier = other.modifier;
		this.obfDescriptor = translator.getTranslatedMethodDesc(other.obfDescriptor);
		this.localVariables = Maps.newTreeMap();
		for (Map.Entry<Integer, LocalVariableMapping> entry : other.localVariables.entrySet()) {
			this.localVariables.put(entry.getKey(), new LocalVariableMapping(entry.getValue()));
		}
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
		if (deobfName == null) {
			return obfName;
		}
		return this.deobfName;
	}

	public void setDeobfName(String val) {
		this.deobfName = NameValidator.validateMethodName(val);
	}

	public MethodDescriptor getObfDesc() {
		return this.obfDescriptor;
	}

	public void setObfDescriptor(MethodDescriptor val) {
		this.obfDescriptor = val;
	}

	public Iterable<LocalVariableMapping> arguments() {
		return this.localVariables.values();
	}

	public void addArgumentMapping(LocalVariableMapping localVariableMapping) throws MappingConflict {
		if (this.localVariables.containsKey(localVariableMapping.getIndex())) {
			throw new MappingConflict("argument", localVariableMapping.getName(), this.localVariables.get(localVariableMapping.getIndex()).getName());
		}
		this.localVariables.put(localVariableMapping.getIndex(), localVariableMapping);
	}

	public String getObfLocalVariableName(int index) {
		LocalVariableMapping localVariableMapping = this.localVariables.get(index);
		if (localVariableMapping != null) {
			return localVariableMapping.getName();
		}

		return null;
	}

	public String getDeobfLocalVariableName(int index) {
		LocalVariableMapping localVariableMapping = this.localVariables.get(index);
		if (localVariableMapping != null) {
			return localVariableMapping.getName();
		}

		return null;
	}

	public void setLocalVariableName(int index, String name) {
		LocalVariableMapping localVariableMapping = this.localVariables.get(index);
		if (localVariableMapping == null) {
			localVariableMapping = new LocalVariableMapping(index, name);
			boolean wasAdded = this.localVariables.put(index, localVariableMapping) == null;
			assert (wasAdded);
		} else {
			localVariableMapping.setName(name);
		}
	}

	public void removeLocalVariableName(int index) {
		boolean wasRemoved = this.localVariables.remove(index) != null;
		assert (wasRemoved);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("\t");
		buf.append(this.obfName);
		buf.append(" <-> ");
		buf.append(this.deobfName);
		buf.append("\n");
		buf.append("\t");
		buf.append(this.obfDescriptor);
		buf.append("\n");
		buf.append("\tLocal Variables:\n");
		for (LocalVariableMapping localVariableMapping : this.localVariables.values()) {
			buf.append("\t\t");
			buf.append(localVariableMapping.getIndex());
			buf.append(" -> ");
			buf.append(localVariableMapping.getName());
			buf.append("\n");
		}
		return buf.toString();
	}

	@Override
	public int compareTo(MethodMapping other) {
		return (this.obfName + this.obfDescriptor).compareTo(other.obfName + other.obfDescriptor);
	}

	public boolean containsLocalVariable(String name) {
		for (LocalVariableMapping localVariableMapping : this.localVariables.values()) {
			if (localVariableMapping.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {
		// rename obf classes in the signature
		MethodDescriptor newDescriptor = obfDescriptor.remap(className -> {
			if (className.equals(oldObfClassName)) {
				return newObfClassName;
			}
			return className;
		});

		if (!newDescriptor.equals(this.obfDescriptor)) {
			this.obfDescriptor = newDescriptor;
			return true;
		}
		return false;
	}

	@Override
	public MethodEntry getObfEntry(ClassEntry classEntry) {
		return new MethodEntry(classEntry, this.obfName, this.obfDescriptor);
	}

	public Mappings.EntryModifier getModifier() {
		return modifier;
	}

	public void setModifier(Mappings.EntryModifier modifier) {
		this.modifier = modifier;
	}

	public boolean isObfuscated() {
		return deobfName == null || deobfName.equals(obfName);
	}
}
