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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cuchaz.enigma.analysis.TranslationIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.entry.ClassEntry;
import cuchaz.enigma.mapping.entry.MethodEntry;
import cuchaz.enigma.throwables.MappingConflict;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Mappings {

	private final FormatType originMapping;
	protected Map<String, ClassMapping> classesByObf;
	protected Map<String, ClassMapping> classesByDeobf;
	private Mappings previousState;

	public Mappings() {
		this(FormatType.ENIGMA_DIRECTORY);
	}

	public Mappings(FormatType originMapping) {
		this.originMapping = originMapping;
		this.classesByObf = Maps.newHashMap();
		this.classesByDeobf = Maps.newHashMap();
	}

	public Collection<ClassMapping> classes() {
		assert (this.classesByObf.size() >= this.classesByDeobf.size());
		return this.classesByObf.values();
	}

	public void addClassMapping(ClassMapping classMapping) throws MappingConflict {
		if (this.classesByObf.containsKey(classMapping.getObfFullName())) {
			throw new MappingConflict("class", classMapping.getObfFullName(), this.classesByObf.get(classMapping.getObfFullName()).getObfFullName());
		}
		this.classesByObf.put(classMapping.getObfFullName(), classMapping);

		if (classMapping.getDeobfName() != null) {
			if (this.classesByDeobf.containsKey(classMapping.getDeobfName())) {
				throw new MappingConflict("class", classMapping.getDeobfName(), this.classesByDeobf.get(classMapping.getDeobfName()).getDeobfName());
			}
			this.classesByDeobf.put(classMapping.getDeobfName(), classMapping);
		}
	}

	public void removeClassMapping(ClassMapping classMapping) {
		boolean obfWasRemoved = this.classesByObf.remove(classMapping.getObfFullName()) != null;
		assert (obfWasRemoved);
		if (classMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = this.classesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (deobfWasRemoved);
		}
	}

	public ClassMapping getClassByObf(ClassEntry entry) {
		return getClassByObf(entry.getName());
	}

	public ClassMapping getClassByObf(String obfName) {
		return this.classesByObf.get(obfName);
	}

	public ClassMapping getClassByDeobf(ClassEntry entry) {
		return getClassByDeobf(entry.getName());
	}

	public ClassMapping getClassByDeobf(String deobfName) {
		return this.classesByDeobf.get(deobfName);
	}

	public void setClassDeobfName(ClassMapping classMapping, String deobfName) {
		if (classMapping.getDeobfName() != null) {
			boolean wasRemoved = this.classesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (wasRemoved);
		}
		classMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			boolean wasAdded = this.classesByDeobf.put(deobfName, classMapping) == null;
			assert (wasAdded);
		}
	}

	public Translator getTranslator(TranslationDirection direction, TranslationIndex index) {
		switch (direction) {
			case DEOBFUSCATING:

				return new DirectionalTranslator(direction, this.classesByObf, index);

			case OBFUSCATING:

				// fill in the missing deobf class entries with obf entries
				Map<String, ClassMapping> classes = Maps.newHashMap();
				for (ClassMapping classMapping : classes()) {
					if (classMapping.getDeobfName() != null) {
						classes.put(classMapping.getDeobfName(), classMapping);
					} else {
						classes.put(classMapping.getObfFullName(), classMapping);
					}
				}

				// translate the translation index
				// NOTE: this isn't actually recursive
				TranslationIndex deobfIndex = new TranslationIndex(index, getTranslator(TranslationDirection.DEOBFUSCATING, index));

				return new DirectionalTranslator(direction, classes, deobfIndex);

			default:
				throw new Error("Invalid translation direction!");
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (ClassMapping classMapping : this.classesByObf.values()) {
			buf.append(classMapping);
			buf.append("\n");
		}
		return buf.toString();
	}

	public void renameObfClass(String oldObfName, String newObfName) {
		new ArrayList<>(classes()).stream().filter(classMapping -> classMapping.renameObfClass(oldObfName, newObfName)).forEach(classMapping -> {
			boolean wasRemoved = this.classesByObf.remove(oldObfName) != null;
			assert (wasRemoved);
			boolean wasAdded = this.classesByObf.put(newObfName, classMapping) == null;
			assert (wasAdded);
		});
	}

	public Set<String> getAllObfClassNames() {
		final Set<String> classNames = Sets.newHashSet();
		for (ClassMapping classMapping : classes()) {

			// add the class name
			classNames.add(classMapping.getObfFullName());

			// add classes from method signatures
			for (MethodMapping methodMapping : classMapping.methods()) {
				for (TypeDescriptor desc : methodMapping.getObfDesc().types()) {
					if (desc.containsType()) {
						classNames.add(desc.getTypeEntry().getClassName());
					}
				}
			}
		}
		return classNames;
	}

	public boolean containsDeobfClass(String deobfName) {
		return this.classesByDeobf.containsKey(deobfName);
	}

	public boolean containsDeobfField(ClassEntry obfClassEntry, String deobfName, TypeDescriptor obfDesc) {
		ClassMapping classMapping = this.classesByObf.get(obfClassEntry.getName());
		return classMapping != null && classMapping.containsDeobfField(deobfName, obfDesc);
	}

	public boolean containsDeobfField(ClassEntry obfClassEntry, String deobfName) {
		ClassMapping classMapping = this.classesByObf.get(obfClassEntry.getName());
		if (classMapping != null)
			for (FieldMapping fieldMapping : classMapping.fields())
				if (deobfName.equals(fieldMapping.getDeobfName()) || deobfName.equals(fieldMapping.getObfName()))
					return true;

		return false;
	}

	public boolean containsDeobfMethod(ClassEntry obfClassEntry, String deobfName, MethodDescriptor obfDescriptor) {
		ClassMapping classMapping = this.classesByObf.get(obfClassEntry.getName());
		return classMapping != null && classMapping.containsDeobfMethod(deobfName, obfDescriptor);
	}

	public boolean containsArgument(MethodEntry obfMethodEntry, String name) {
		ClassMapping classMapping = this.classesByObf.get(obfMethodEntry.getClassName());
		return classMapping != null && classMapping.containsArgument(obfMethodEntry, name);
	}

	public List<ClassMapping> getClassMappingChain(ClassEntry obfClass) {
		List<ClassMapping> mappingChain = Lists.newArrayList();
		ClassMapping classMapping = null;
		for (ClassEntry obfClassEntry : obfClass.getClassChain()) {
			if (mappingChain.isEmpty()) {
				classMapping = this.classesByObf.get(obfClassEntry.getName());
			} else if (classMapping != null) {
				classMapping = classMapping.getInnerClassByObfSimple(obfClassEntry.getInnermostClassName());
			}
			mappingChain.add(classMapping);
		}
		return mappingChain;
	}

	public FormatType getOriginMappingFormat() {
		return originMapping;
	}

	public void savePreviousState() {
		this.previousState = new Mappings(this.originMapping);
		this.previousState.classesByDeobf = new HashMap<>();
		for (Map.Entry<String, ClassMapping> entry : this.classesByDeobf.entrySet()) {
			this.previousState.classesByDeobf.put(entry.getKey(), entry.getValue().copy());
		}
		this.previousState.classesByObf = new HashMap<>();
		for (Map.Entry<String, ClassMapping> entry : this.classesByObf.entrySet()) {
			this.previousState.classesByObf.put(entry.getKey(), entry.getValue().copy());
		}
		classesByDeobf.values().forEach(ClassMapping::resetDirty);
		classesByObf.values().forEach(ClassMapping::resetDirty);
	}

	public void saveEnigmaMappings(File file, boolean isDirectoryFormat) throws IOException {
		new MappingsEnigmaWriter().write(file, this, isDirectoryFormat);
		this.savePreviousState();
	}

	public void saveSRGMappings(File file) throws IOException {
		new MappingsSRGWriter().write(file, this);
	}

	public Mappings getPreviousState() {
		return previousState;
	}

	public enum FormatType {
		ENIGMA_FILE, ENIGMA_DIRECTORY, TINY_FILE, SRG_FILE
	}

	public enum EntryModifier {
		UNCHANGED, PUBLIC, PROTECTED, PRIVATE;

		public String getFormattedName() {
			return " ACC:" + super.toString();
		}

		public AccessFlags transform(AccessFlags access) {
			switch (this) {
				case PUBLIC:
					return access.setPublic();
				case PROTECTED:
					return access.setProtected();
				case PRIVATE:
					return access.setPrivate();
				case UNCHANGED:
				default:
					return access;
			}
		}
	}
}
