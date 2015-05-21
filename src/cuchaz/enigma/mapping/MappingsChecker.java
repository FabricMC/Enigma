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

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.analysis.RelatedMethodChecker;


public class MappingsChecker {
	
	private JarIndex m_index;
	private RelatedMethodChecker m_relatedMethodChecker;
	private Map<ClassEntry,ClassMapping> m_droppedClassMappings;
	private Map<ClassEntry,ClassMapping> m_droppedInnerClassMappings;
	private Map<FieldEntry,FieldMapping> m_droppedFieldMappings;
	private Map<BehaviorEntry,MethodMapping> m_droppedMethodMappings;
	
	public MappingsChecker(JarIndex index) {
		m_index = index;
		m_relatedMethodChecker = new RelatedMethodChecker(m_index);
		m_droppedClassMappings = Maps.newHashMap();
		m_droppedInnerClassMappings = Maps.newHashMap();
		m_droppedFieldMappings = Maps.newHashMap();
		m_droppedMethodMappings = Maps.newHashMap();
	}
	
	public RelatedMethodChecker getRelatedMethodChecker() {
		return m_relatedMethodChecker;
	}
	
	public Map<ClassEntry,ClassMapping> getDroppedClassMappings() {
		return m_droppedClassMappings;
	}
	
	public Map<ClassEntry,ClassMapping> getDroppedInnerClassMappings() {
		return m_droppedInnerClassMappings;
	}
	
	public Map<FieldEntry,FieldMapping> getDroppedFieldMappings() {
		return m_droppedFieldMappings;
	}
	
	public Map<BehaviorEntry,MethodMapping> getDroppedMethodMappings() {
		return m_droppedMethodMappings;
	}
	
	public void dropBrokenMappings(Mappings mappings) {
		for (ClassMapping classMapping : Lists.newArrayList(mappings.classes())) {
			if (!checkClassMapping(classMapping)) {
				mappings.removeClassMapping(classMapping);
				m_droppedClassMappings.put(EntryFactory.getObfClassEntry(m_index, classMapping), classMapping);
			}
		}
	}
	
	private boolean checkClassMapping(ClassMapping classMapping) {
		
		// check the class
		ClassEntry classEntry = EntryFactory.getObfClassEntry(m_index, classMapping);
		if (!m_index.getObfClassEntries().contains(classEntry)) {
			return false;
		}
		
		// check the fields
		for (FieldMapping fieldMapping : Lists.newArrayList(classMapping.fields())) {
			FieldEntry obfFieldEntry = EntryFactory.getObfFieldEntry(classMapping, fieldMapping);
			if (!m_index.containsObfField(obfFieldEntry)) {
				classMapping.removeFieldMapping(fieldMapping);
				m_droppedFieldMappings.put(obfFieldEntry, fieldMapping);
			}
		}
		
		// check methods
		for (MethodMapping methodMapping : Lists.newArrayList(classMapping.methods())) {
			BehaviorEntry obfBehaviorEntry = EntryFactory.getObfBehaviorEntry(classEntry, methodMapping);
			if (!m_index.containsObfBehavior(obfBehaviorEntry)) {
				classMapping.removeMethodMapping(methodMapping);
				m_droppedMethodMappings.put(obfBehaviorEntry, methodMapping);
			}
			
			m_relatedMethodChecker.checkMethod(classEntry, methodMapping);
		}
		
		// check inner classes
		for (ClassMapping innerClassMapping : Lists.newArrayList(classMapping.innerClasses())) {
			if (!checkClassMapping(innerClassMapping)) {
				classMapping.removeInnerClassMapping(innerClassMapping);
				m_droppedInnerClassMappings.put(EntryFactory.getObfClassEntry(m_index, innerClassMapping), innerClassMapping);
			}
		}
		
		return true;
	}
}
