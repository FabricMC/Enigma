/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.enigma.analysis.TranslationIndex;

public class Mappings implements Serializable {
	
	private static final long serialVersionUID = 4649790259460259026L;
	
	protected Map<String,ClassMapping> m_classesByObf;
	protected Map<String,ClassMapping> m_classesByDeobf;
	
	public Mappings() {
		m_classesByObf = Maps.newHashMap();
		m_classesByDeobf = Maps.newHashMap();
	}
	
	public Mappings(Iterable<ClassMapping> classes) {
		this();
		
		for (ClassMapping classMapping : classes) {
			m_classesByObf.put(classMapping.getObfFullName(), classMapping);
			if (classMapping.getDeobfName() != null) {
				m_classesByDeobf.put(classMapping.getDeobfName(), classMapping);
			}
		}
	}
	
	public Collection<ClassMapping> classes() {
		assert (m_classesByObf.size() >= m_classesByDeobf.size());
		return m_classesByObf.values();
	}
	
	public void addClassMapping(ClassMapping classMapping) {
		if (m_classesByObf.containsKey(classMapping.getObfFullName())) {
			throw new Error("Already have mapping for " + classMapping.getObfFullName());
		}
		boolean obfWasAdded = m_classesByObf.put(classMapping.getObfFullName(), classMapping) == null;
		assert (obfWasAdded);
		if (classMapping.getDeobfName() != null) {
			if (m_classesByDeobf.containsKey(classMapping.getDeobfName())) {
				throw new Error("Already have mapping for " + classMapping.getDeobfName());
			}
			boolean deobfWasAdded = m_classesByDeobf.put(classMapping.getDeobfName(), classMapping) == null;
			assert (deobfWasAdded);
		}
	}
	
	public void removeClassMapping(ClassMapping classMapping) {
		boolean obfWasRemoved = m_classesByObf.remove(classMapping.getObfFullName()) != null;
		assert (obfWasRemoved);
		if (classMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = m_classesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (deobfWasRemoved);
		}
	}
	
	public ClassMapping getClassByObf(ClassEntry entry) {
		return getClassByObf(entry.getName());
	}
	
	public ClassMapping getClassByObf(String obfName) {
		return m_classesByObf.get(obfName);
	}
	
	public ClassMapping getClassByDeobf(ClassEntry entry) {
		return getClassByDeobf(entry.getName());
	}
	
	public ClassMapping getClassByDeobf(String deobfName) {
		return m_classesByDeobf.get(deobfName);
	}
	
	public void setClassDeobfName(ClassMapping classMapping, String deobfName) {
		if (classMapping.getDeobfName() != null) {
			boolean wasRemoved = m_classesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (wasRemoved);
		}
		classMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			boolean wasAdded = m_classesByDeobf.put(deobfName, classMapping) == null;
			assert (wasAdded);
		}
	}
	
	public Translator getTranslator(TranslationDirection direction, TranslationIndex index) {
		switch (direction) {
			case Deobfuscating:
				
				return new Translator(direction, m_classesByObf, index);
				
			case Obfuscating:
				
				// fill in the missing deobf class entries with obf entries
				Map<String,ClassMapping> classes = Maps.newHashMap();
				for (ClassMapping classMapping : classes()) {
					if (classMapping.getDeobfName() != null) {
						classes.put(classMapping.getDeobfName(), classMapping);
					} else {
						classes.put(classMapping.getObfFullName(), classMapping);
					}
				}
				
				// translate the translation index
				// NOTE: this isn't actually recursive
				TranslationIndex deobfIndex = new TranslationIndex(index, getTranslator(TranslationDirection.Deobfuscating, index));
				
				return new Translator(direction, classes, deobfIndex);
				
			default:
				throw new Error("Invalid translation direction!");
		}
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (ClassMapping classMapping : m_classesByObf.values()) {
			buf.append(classMapping.toString());
			buf.append("\n");
		}
		return buf.toString();
	}
	
	public void renameObfClass(String oldObfName, String newObfName) {
		for (ClassMapping classMapping : new ArrayList<ClassMapping>(classes())) {
			if (classMapping.renameObfClass(oldObfName, newObfName)) {
				boolean wasRemoved = m_classesByObf.remove(oldObfName) != null;
				assert (wasRemoved);
				boolean wasAdded = m_classesByObf.put(newObfName, classMapping) == null;
				assert (wasAdded);
			}
		}
	}
	
	public Set<String> getAllObfClassNames() {
		final Set<String> classNames = Sets.newHashSet();
		for (ClassMapping classMapping : classes()) {
			
			// add the class name
			classNames.add(classMapping.getObfFullName());
			
			// add classes from method signatures
			for (MethodMapping methodMapping : classMapping.methods()) {
				for (Type type : methodMapping.getObfSignature().types()) {
					if (type.hasClass()) {
						classNames.add(type.getClassEntry().getClassName());
					}
				}
			}
		}
		return classNames;
	}
	
	public boolean containsDeobfClass(String deobfName) {
		return m_classesByDeobf.containsKey(deobfName);
	}
	
	public boolean containsDeobfField(ClassEntry obfClassEntry, String deobfName, Type obfType) {
		ClassMapping classMapping = m_classesByObf.get(obfClassEntry.getName());
		if (classMapping != null) {
			return classMapping.containsDeobfField(deobfName, obfType);
		}
		return false;
	}
	
	public boolean containsDeobfMethod(ClassEntry obfClassEntry, String deobfName, Signature deobfSignature) {
		ClassMapping classMapping = m_classesByObf.get(obfClassEntry.getName());
		if (classMapping != null) {
			return classMapping.containsDeobfMethod(deobfName, deobfSignature);
		}
		return false;
	}
	
	public boolean containsArgument(BehaviorEntry obfBehaviorEntry, String name) {
		ClassMapping classMapping = m_classesByObf.get(obfBehaviorEntry.getClassName());
		if (classMapping != null) {
			return classMapping.containsArgument(obfBehaviorEntry, name);
		}
		return false;
	}
	
	public List<ClassMapping> getClassMappingChain(ClassEntry obfClass) {
		List<ClassMapping> mappingChain = Lists.newArrayList();
		ClassMapping classMapping = null;
		for (ClassEntry obfClassEntry : obfClass.getClassChain()) {
			if (mappingChain.isEmpty()) {
				classMapping = m_classesByObf.get(obfClassEntry.getName());
			} else if (classMapping != null) {
				classMapping = classMapping.getInnerClassByObfSimple(obfClassEntry.getInnermostClassName());
			}
			mappingChain.add(classMapping);
		}
		return mappingChain;
	}
}
