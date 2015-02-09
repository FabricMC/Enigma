/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import cuchaz.enigma.Constants;
import cuchaz.enigma.analysis.JarIndex;

public class MappingsRenamer {
	
	private JarIndex m_index;
	private Mappings m_mappings;
	
	public MappingsRenamer(JarIndex index, Mappings mappings) {
		m_index = index;
		m_mappings = mappings;
	}
	
	public void setClassName(ClassEntry obf, String deobfName) {
		deobfName = NameValidator.validateClassName(deobfName, !obf.isInnerClass());
		ClassEntry targetEntry = new ClassEntry(deobfName);
		if (m_mappings.containsDeobfClass(deobfName) || m_index.containsObfClass(targetEntry)) {
			throw new IllegalNameException(deobfName, "There is already a class with that name");
		}
		
		ClassMapping classMapping = getOrCreateClassMapping(obf);
		
		if (obf.isInnerClass()) {
			classMapping.setInnerClassName(obf.getInnerClassName(), deobfName);
		} else {
			if (classMapping.getDeobfName() != null) {
				boolean wasRemoved = m_mappings.m_classesByDeobf.remove(classMapping.getDeobfName()) != null;
				assert (wasRemoved);
			}
			classMapping.setDeobfName(deobfName);
			boolean wasAdded = m_mappings.m_classesByDeobf.put(deobfName, classMapping) == null;
			assert (wasAdded);
		}
	}
	
	public void removeClassMapping(ClassEntry obf) {
		ClassMapping classMapping = getClassMapping(obf);
		if (obf.isInnerClass()) {
			classMapping.setInnerClassName(obf.getName(), null);
		} else {
			boolean wasRemoved = m_mappings.m_classesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (wasRemoved);
			classMapping.setDeobfName(null);
		}
	}
	
	public void markClassAsDeobfuscated(ClassEntry obf) {
		ClassMapping classMapping = getOrCreateClassMapping(obf);
		if (obf.isInnerClass()) {
			String innerClassName = Constants.NonePackage + "/" + obf.getInnerClassName();
			classMapping.setInnerClassName(innerClassName, innerClassName);
		} else {
			classMapping.setDeobfName(obf.getName());
			boolean wasAdded = m_mappings.m_classesByDeobf.put(obf.getName(), classMapping) == null;
			assert (wasAdded);
		}
	}
	
	public void setFieldName(FieldEntry obf, String deobfName) {
		deobfName = NameValidator.validateFieldName(deobfName);
		FieldEntry targetEntry = new FieldEntry(obf.getClassEntry(), deobfName);
		if (m_mappings.containsDeobfField(obf.getClassEntry(), deobfName) || m_index.containsObfField(targetEntry)) {
			throw new IllegalNameException(deobfName, "There is already a field with that name");
		}
		
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setFieldName(obf.getName(), deobfName);
	}
	
	public void removeFieldMapping(FieldEntry obf) {
		ClassMapping classMapping = getClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setFieldName(obf.getName(), null);
	}
	
	public void markFieldAsDeobfuscated(FieldEntry obf) {
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setFieldName(obf.getName(), obf.getName());
	}
	
	public void setMethodTreeName(MethodEntry obf, String deobfName) {
		Set<MethodEntry> implementations = m_index.getRelatedMethodImplementations(obf);
		
		deobfName = NameValidator.validateMethodName(deobfName);
		for (MethodEntry entry : implementations) {
			Signature deobfSignature = m_mappings.getTranslator(TranslationDirection.Deobfuscating, m_index.getTranslationIndex()).translateSignature(obf.getSignature());
			MethodEntry targetEntry = new MethodEntry(entry.getClassEntry(), deobfName, deobfSignature);
			if (m_mappings.containsDeobfMethod(entry.getClassEntry(), deobfName, entry.getSignature()) || m_index.containsObfBehavior(targetEntry)) {
				String deobfClassName = m_mappings.getTranslator(TranslationDirection.Deobfuscating, m_index.getTranslationIndex()).translateClass(entry.getClassName());
				throw new IllegalNameException(deobfName, "There is already a method with that name and signature in class " + deobfClassName);
			}
		}
		
		for (MethodEntry entry : implementations) {
			setMethodName(entry, deobfName);
		}
	}
	
	public void setMethodName(MethodEntry obf, String deobfName) {
		deobfName = NameValidator.validateMethodName(deobfName);
		MethodEntry targetEntry = new MethodEntry(obf.getClassEntry(), deobfName, obf.getSignature());
		if (m_mappings.containsDeobfMethod(obf.getClassEntry(), deobfName, obf.getSignature()) || m_index.containsObfBehavior(targetEntry)) {
			String deobfClassName = m_mappings.getTranslator(TranslationDirection.Deobfuscating, m_index.getTranslationIndex()).translateClass(obf.getClassName());
			throw new IllegalNameException(deobfName, "There is already a method with that name and signature in class " + deobfClassName);
		}
		
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setMethodName(obf.getName(), obf.getSignature(), deobfName);
	}
	
	public void removeMethodTreeMapping(MethodEntry obf) {
		for (MethodEntry implementation : m_index.getRelatedMethodImplementations(obf)) {
			removeMethodMapping(implementation);
		}
	}
	
	public void removeMethodMapping(MethodEntry obf) {
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setMethodName(obf.getName(), obf.getSignature(), null);
	}
	
	public void markMethodTreeAsDeobfuscated(MethodEntry obf) {
		for (MethodEntry implementation : m_index.getRelatedMethodImplementations(obf)) {
			markMethodAsDeobfuscated(implementation);
		}
	}
	
	public void markMethodAsDeobfuscated(MethodEntry obf) {
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setMethodName(obf.getName(), obf.getSignature(), obf.getName());
	}
	
	public void setArgumentName(ArgumentEntry obf, String deobfName) {
		deobfName = NameValidator.validateArgumentName(deobfName);
		// NOTE: don't need to check arguments for name collisions with names determined by Procyon
		if (m_mappings.containsArgument(obf.getBehaviorEntry(), deobfName)) {
			throw new IllegalNameException(deobfName, "There is already an argument with that name");
		}
		
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setArgumentName(obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), deobfName);
	}
	
	public void removeArgumentMapping(ArgumentEntry obf) {
		ClassMapping classMapping = getClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.removeArgumentName(obf.getMethodName(), obf.getMethodSignature(), obf.getIndex());
	}
	
	public void markArgumentAsDeobfuscated(ArgumentEntry obf) {
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping(obf.getClassEntry());
		classMapping.setArgumentName(obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), obf.getName());
	}
	
	public boolean moveFieldToObfClass(ClassMapping classMapping, FieldMapping fieldMapping, ClassEntry obfClass) {
		classMapping.removeFieldMapping(fieldMapping);
		ClassMapping targetClassMapping = getOrCreateClassMapping(obfClass);
		if (!targetClassMapping.containsObfField(fieldMapping.getObfName())) {
			if (!targetClassMapping.containsDeobfField(fieldMapping.getDeobfName())) {
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
		if (!targetClassMapping.containsObfMethod(methodMapping.getObfName(), methodMapping.getObfSignature())) {
			if (!targetClassMapping.containsDeobfMethod(methodMapping.getDeobfName(), methodMapping.getObfSignature())) {
				targetClassMapping.addMethodMapping(methodMapping);
				return true;
			} else {
				System.err.println("WARNING: deobf method was already there: " + obfClass + "." + methodMapping.getDeobfName() + methodMapping.getObfSignature());
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
	
	private ClassMapping getClassMapping(ClassEntry obfClassEntry) {
		return m_mappings.m_classesByObf.get(obfClassEntry.getOuterClassName());
	}
	
	private ClassMapping getOrCreateClassMapping(ClassEntry obfClassEntry) {
		String obfClassName = obfClassEntry.getOuterClassName();
		ClassMapping classMapping = m_mappings.m_classesByObf.get(obfClassName);
		if (classMapping == null) {
			classMapping = new ClassMapping(obfClassName);
			boolean obfWasAdded = m_mappings.m_classesByObf.put(classMapping.getObfName(), classMapping) == null;
			assert (obfWasAdded);
		}
		return classMapping;
	}
	
	private ClassMapping getClassMappingOrInnerClassMapping(ClassEntry obfClassEntry) {
		ClassMapping classMapping = getClassMapping(obfClassEntry);
		if (obfClassEntry.isInDefaultPackage()) {
			classMapping = classMapping.getInnerClassByObf(obfClassEntry.getInnerClassName());
		}
		return classMapping;
	}
	
	private ClassMapping getOrCreateClassMappingOrInnerClassMapping(ClassEntry obfClassEntry) {
		ClassMapping classMapping = getOrCreateClassMapping(obfClassEntry);
		if (obfClassEntry.isInnerClass()) {
			classMapping = classMapping.getOrCreateInnerClass(obfClassEntry.getInnerClassName());
		}
		return classMapping;
	}
}
