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
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.entry.*;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.throwables.MappingConflict;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class MappingsRenamer {

	private final JarIndex index;
	private final ReferencedEntryPool entryPool;
	private Mappings mappings;

	public MappingsRenamer(JarIndex index, Mappings mappings, ReferencedEntryPool entryPool) {
		this.index = index;
		this.mappings = mappings;
		this.entryPool = entryPool;
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public void setClassName(ClassEntry obf, String deobfName) {

		deobfName = NameValidator.validateClassName(deobfName.replace('.', '/'), !obf.isInnerClass());

		List<ClassMapping> mappingChain = getOrCreateClassMappingChain(obf);
		if (mappingChain.size() == 1) {

			if (deobfName != null) {
				// make sure we don't rename to an existing obf or deobf class
				if (mappings.containsDeobfClass(deobfName) || index.containsObfClass(entryPool.getClass(deobfName))) {
					throw new IllegalNameException(deobfName, "There is already a class with that name");
				}
			}

			ClassMapping classMapping = mappingChain.get(0);
			mappings.setClassDeobfName(classMapping, deobfName);

		} else {

			ClassMapping outerClassMapping = mappingChain.get(mappingChain.size() - 2);

			if (deobfName != null) {
				// make sure we don't rename to an existing obf or deobf inner class
				if (outerClassMapping.hasInnerClassByDeobf(deobfName) || outerClassMapping.hasInnerClassByObfSimple(deobfName)) {
					throw new IllegalNameException(deobfName, "There is already a class with that name");
				}
			}

			outerClassMapping.setInnerClassName(obf, deobfName);
		}
	}

	public void removeClassMapping(ClassEntry obf) {
		setClassName(obf, null);
	}

	public void markClassAsDeobfuscated(ClassEntry obf) {
		String deobfName = obf.isInnerClass() ? obf.getInnermostClassName() : obf.getName();
		List<ClassMapping> mappingChain = getOrCreateClassMappingChain(obf);
		if (mappingChain.size() == 1) {
			ClassMapping classMapping = mappingChain.get(0);
			mappings.setClassDeobfName(classMapping, deobfName);
		} else {
			ClassMapping outerClassMapping = mappingChain.get(mappingChain.size() - 2);
			outerClassMapping.setInnerClassName(obf, deobfName);
		}
	}

	public void setFieldName(FieldEntry obf, String deobfName) {
		deobfName = NameValidator.validateFieldName(deobfName);
		FieldEntry targetEntry = entryPool.getField(obf.getOwnerClassEntry(), deobfName, obf.getDesc());
		ClassEntry definedClass = null;
		if (mappings.containsDeobfField(obf.getOwnerClassEntry(), deobfName) || index.containsEntryWithSameName(targetEntry))
			definedClass = obf.getOwnerClassEntry();
		else {
			for (ClassEntry ancestorEntry : this.index.getTranslationIndex().getAncestry(obf.getOwnerClassEntry())) {
				if (mappings.containsDeobfField(ancestorEntry, deobfName) || index.containsEntryWithSameName(targetEntry.updateOwnership(ancestorEntry))) {
					definedClass = ancestorEntry;
					break;
				}
			}
		}

		if (definedClass != null) {
			Translator translator = mappings.getTranslator(TranslationDirection.DEOBFUSCATING, index.getTranslationIndex());
			String className = translator.getTranslatedClass(entryPool.getClass(definedClass.getClassName())).getName();
			if (className == null)
				className = definedClass.getClassName();
			throw new IllegalNameException(deobfName, "There is already a field with that name in " + className);
		}

		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.setFieldName(obf.getName(), obf.getDesc(), deobfName);
	}

	public void removeFieldMapping(FieldEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.removeFieldMapping(classMapping.getFieldByObf(obf.getName(), obf.getDesc()));
	}

	public void markFieldAsDeobfuscated(FieldEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.setFieldName(obf.getName(), obf.getDesc(), obf.getName());
	}

	private void validateMethodTreeName(MethodEntry entry, String deobfName) {
		MethodEntry targetEntry = entryPool.getMethod(entry.getOwnerClassEntry(), deobfName, entry.getDesc());

		// TODO: Verify if I don't break things
		ClassMapping classMapping = mappings.getClassByObf(entry.getOwnerClassEntry());
		if ((classMapping != null && classMapping.containsDeobfMethod(deobfName, entry.getDesc()) && classMapping.getMethodByObf(entry.getName(), entry.getDesc()) != classMapping.getMethodByDeobf(deobfName, entry.getDesc()))
				|| index.containsObfMethod(targetEntry)) {
			Translator translator = mappings.getTranslator(TranslationDirection.DEOBFUSCATING, index.getTranslationIndex());
			String deobfClassName = translator.getTranslatedClass(entryPool.getClass(entry.getClassName())).getClassName();
			if (deobfClassName == null) {
				deobfClassName = entry.getClassName();
			}
			throw new IllegalNameException(deobfName, "There is already a method with that name and signature in class " + deobfClassName);
		}

		for (ClassEntry child : index.getTranslationIndex().getSubclass(entry.getOwnerClassEntry())) {
			validateMethodTreeName(entry.updateOwnership(child), deobfName);
		}
	}

	public void setMethodTreeName(MethodEntry obf, String deobfName) {
		Set<MethodEntry> implementations = index.getRelatedMethodImplementations(obf);

		deobfName = NameValidator.validateMethodName(deobfName);
		for (MethodEntry entry : implementations) {
			validateMethodTreeName(entry, deobfName);
		}

		for (MethodEntry entry : implementations) {
			setMethodName(entry, deobfName);
		}
	}

	public void setMethodName(MethodEntry obf, String deobfName) {
		deobfName = NameValidator.validateMethodName(deobfName);
		MethodEntry targetEntry = entryPool.getMethod(obf.getOwnerClassEntry(), deobfName, obf.getDesc());
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());

		// TODO: Verify if I don't break things
		if ((mappings.containsDeobfMethod(obf.getOwnerClassEntry(), deobfName, obf.getDesc()) && classMapping.getMethodByObf(obf.getName(), obf.getDesc()) != classMapping.getMethodByDeobf(deobfName, obf.getDesc()))
				|| index.containsObfMethod(targetEntry)) {
			Translator translator = mappings.getTranslator(TranslationDirection.DEOBFUSCATING, index.getTranslationIndex());
			String deobfClassName = translator.getTranslatedClass(entryPool.getClass(obf.getClassName())).getClassName();
			if (deobfClassName == null) {
				deobfClassName = obf.getClassName();
			}
			throw new IllegalNameException(deobfName, "There is already a method with that name and signature in class " + deobfClassName);
		}

		classMapping.setMethodName(obf.getName(), obf.getDesc(), deobfName);
	}

	public void removeMethodTreeMapping(MethodEntry obf) {
		index.getRelatedMethodImplementations(obf).forEach(this::removeMethodMapping);
	}

	public void removeMethodMapping(MethodEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.setMethodName(obf.getName(), obf.getDesc(), null);
	}

	public void markMethodTreeAsDeobfuscated(MethodEntry obf) {
		index.getRelatedMethodImplementations(obf).forEach(this::markMethodAsDeobfuscated);
	}

	public void markMethodAsDeobfuscated(MethodEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.setMethodName(obf.getName(), obf.getDesc(), obf.getName());
	}

	public void setLocalVariableTreeName(LocalVariableEntry obf, String deobfName) {
		MethodEntry obfMethod = obf.getOwnerEntry();
		if (!obf.isParameter()) {
			setLocalVariableName(obf, deobfName);
			return;
		}

		Set<MethodEntry> implementations = index.getRelatedMethodImplementations(obfMethod);
		for (MethodEntry entry : implementations) {
			ClassMapping classMapping = mappings.getClassByObf(entry.getOwnerClassEntry());
			if (classMapping != null) {
				MethodMapping mapping = classMapping.getMethodByObf(entry.getName(), entry.getDesc());
				// NOTE: don't need to check arguments for name collisions with names determined by Procyon
				// TODO: Verify if I don't break things
				if (mapping != null) {
					for (LocalVariableMapping localVariableMapping : Lists.newArrayList(mapping.arguments())) {
						if (localVariableMapping.getIndex() != obf.getIndex()) {
							if (mapping.getDeobfLocalVariableName(localVariableMapping.getIndex()).equals(deobfName)
									|| localVariableMapping.getName().equals(deobfName)) {
								throw new IllegalNameException(deobfName, "There is already an argument with that name");
							}
						}
					}
				}
			}
		}

		for (MethodEntry entry : implementations) {
			setLocalVariableName(new LocalVariableEntry(entry, obf.getIndex(), obf.getName(), obf.isParameter()), deobfName);
		}
	}

	public void setLocalVariableName(LocalVariableEntry obf, String deobfName) {
		deobfName = NameValidator.validateArgumentName(deobfName);
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		MethodMapping mapping = classMapping.getMethodByObf(obf.getMethodName(), obf.getMethodDesc());
		// NOTE: don't need to check arguments for name collisions with names determined by Procyon
		// TODO: Verify if I don't break things
		if (mapping != null) {
			for (LocalVariableMapping localVariableMapping : Lists.newArrayList(mapping.arguments())) {
				if (localVariableMapping.getIndex() != obf.getIndex()) {
					if (mapping.getDeobfLocalVariableName(localVariableMapping.getIndex()).equals(deobfName)
							|| localVariableMapping.getName().equals(deobfName)) {
						throw new IllegalNameException(deobfName, "There is already an argument with that name");
					}
				}
			}
		}

		classMapping.setArgumentName(obf.getMethodName(), obf.getMethodDesc(), obf.getIndex(), deobfName);
	}

	public void removeLocalVariableMapping(LocalVariableEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.removeArgumentName(obf.getMethodName(), obf.getMethodDesc(), obf.getIndex());
	}

	public void markArgumentAsDeobfuscated(LocalVariableEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf.getOwnerClassEntry());
		classMapping.setArgumentName(obf.getMethodName(), obf.getMethodDesc(), obf.getIndex(), obf.getName());
	}

	public boolean moveFieldToObfClass(ClassMapping classMapping, FieldMapping fieldMapping, ClassEntry obfClass) {
		classMapping.removeFieldMapping(fieldMapping);
		ClassMapping targetClassMapping = getOrCreateClassMapping(obfClass);
		if (!targetClassMapping.containsObfField(fieldMapping.getObfName(), fieldMapping.getObfDesc())) {
			if (!targetClassMapping.containsDeobfField(fieldMapping.getDeobfName(), fieldMapping.getObfDesc())) {
				targetClassMapping.addFieldMapping(fieldMapping);
				return true;
			} else {
				System.err.println("WARNING: deobf field was already there: " + obfClass + "." + fieldMapping.getDeobfName());
			}
		}
		return false;
	}

	public boolean moveMethodToObfClass(ClassMapping classMapping, MethodMapping methodMapping, ClassEntry obfClass) {
		classMapping.removeMethodMapping(methodMapping);
		ClassMapping targetClassMapping = getOrCreateClassMapping(obfClass);
		if (!targetClassMapping.containsObfMethod(methodMapping.getObfName(), methodMapping.getObfDesc())) {
			if (!targetClassMapping.containsDeobfMethod(methodMapping.getDeobfName(), methodMapping.getObfDesc())) {
				targetClassMapping.addMethodMapping(methodMapping);
				return true;
			} else {
				System.err.println("WARNING: deobf method was already there: " + obfClass + "." + methodMapping.getDeobfName() + methodMapping.getObfDesc());
			}
		}
		return false;
	}

	public void write(OutputStream out) throws IOException {
		// TEMP: just use the object output for now. We can find a more efficient storage format later
		GZIPOutputStream gzipout = new GZIPOutputStream(out);
		ObjectOutputStream oout = new ObjectOutputStream(gzipout);
		oout.writeObject(this);
		gzipout.finish();
	}

	private ClassMapping getOrCreateClassMapping(ClassEntry obfClassEntry) {
		List<ClassMapping> mappingChain = getOrCreateClassMappingChain(obfClassEntry);
		return mappingChain.get(mappingChain.size() - 1);
	}

	private List<ClassMapping> getOrCreateClassMappingChain(ClassEntry obfClassEntry) {
		List<ClassEntry> classChain = obfClassEntry.getClassChain();
		List<ClassMapping> mappingChain = mappings.getClassMappingChain(obfClassEntry);
		for (int i = 0; i < classChain.size(); i++) {
			ClassEntry classEntry = classChain.get(i);
			ClassMapping classMapping = mappingChain.get(i);
			if (classMapping == null) {

				// create it
				classMapping = new ClassMapping(classEntry.getName());
				mappingChain.set(i, classMapping);

				// add it to the right parent
				try {
					if (i == 0) {
						mappings.addClassMapping(classMapping);
					} else {
						mappingChain.get(i - 1).addInnerClassMapping(classMapping);
					}
				} catch (MappingConflict mappingConflict) {
					mappingConflict.printStackTrace();
				}
			}
		}
		return mappingChain;
	}

	public void setClassModifier(ClassEntry obEntry, Mappings.EntryModifier modifier) {
		ClassMapping classMapping = getOrCreateClassMapping(obEntry);
		classMapping.setModifier(modifier);
	}

	public void setFieldModifier(FieldEntry obEntry, Mappings.EntryModifier modifier) {
		ClassMapping classMapping = getOrCreateClassMapping(obEntry.getOwnerClassEntry());
		classMapping.setFieldModifier(obEntry.getName(), obEntry.getDesc(), modifier);
	}

	public void setMethodModifier(MethodEntry obEntry, Mappings.EntryModifier modifier) {
		ClassMapping classMapping = getOrCreateClassMapping(obEntry.getOwnerClassEntry());
		classMapping.setMethodModifier(obEntry.getName(), obEntry.getDesc(), modifier);
	}

	public Mappings.EntryModifier getClassModifier(ClassEntry obfEntry) {
		ClassMapping classMapping = getOrCreateClassMapping(obfEntry);
		return classMapping.getModifier();
	}

	public Mappings.EntryModifier getFieldModifier(FieldEntry obfEntry) {
		ClassMapping classMapping = getOrCreateClassMapping(obfEntry.getOwnerClassEntry());
		FieldMapping fieldMapping = classMapping.getFieldByObf(obfEntry);
		if (fieldMapping == null) {
			return Mappings.EntryModifier.UNCHANGED;
		}
		return fieldMapping.getModifier();
	}

	public Mappings.EntryModifier getMethodModfifier(MethodEntry obfEntry) {
		ClassMapping classMapping = getOrCreateClassMapping(obfEntry.getOwnerClassEntry());
		MethodMapping methodMapping = classMapping.getMethodByObf(obfEntry);
		if (methodMapping == null) {
			return Mappings.EntryModifier.UNCHANGED;
		}
		return methodMapping.getModifier();
	}
}
