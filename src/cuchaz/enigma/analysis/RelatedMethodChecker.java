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
package cuchaz.enigma.analysis;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.MethodMapping;

public class RelatedMethodChecker {
	
	private JarIndex m_jarIndex;
	private Map<Set<MethodEntry>,String> m_deobfNamesByGroup;
	private Map<MethodEntry,String> m_deobfNamesByObfMethod;
	private Map<MethodEntry,Set<MethodEntry>> m_groupsByObfMethod;
	private Set<Set<MethodEntry>> m_inconsistentGroups;
	
	public RelatedMethodChecker(JarIndex jarIndex) {
		m_jarIndex = jarIndex;
		m_deobfNamesByGroup = Maps.newHashMap();
		m_deobfNamesByObfMethod = Maps.newHashMap();
		m_groupsByObfMethod = Maps.newHashMap();
		m_inconsistentGroups = Sets.newHashSet();
	}
	
	public void checkMethod(ClassEntry classEntry, MethodMapping methodMapping) {
		
		// TEMP: disable the expensive check for now, maybe we can optimize it later, or just use it for debugging
		if (true) return;
		
		BehaviorEntry obfBehaviorEntry = EntryFactory.getObfBehaviorEntry(classEntry, methodMapping);
		if (!(obfBehaviorEntry instanceof MethodEntry)) {
			// only methods have related implementations
			return;
		}
		MethodEntry obfMethodEntry = (MethodEntry)obfBehaviorEntry;
		String deobfName = methodMapping.getDeobfName();
		m_deobfNamesByObfMethod.put(obfMethodEntry, deobfName);
		
		// have we seen this method's group before?
		Set<MethodEntry> group = m_groupsByObfMethod.get(obfMethodEntry);
		if (group == null) {
			
			// no, compute the group and save the name
			group = m_jarIndex.getRelatedMethodImplementations(obfMethodEntry);
			m_deobfNamesByGroup.put(group, deobfName);
			
			assert(group.contains(obfMethodEntry));
			for (MethodEntry relatedMethodEntry : group) {
				m_groupsByObfMethod.put(relatedMethodEntry, group);
			}
		}
		
		// check the name
		if (!sameName(m_deobfNamesByGroup.get(group), deobfName)) {
			m_inconsistentGroups.add(group);
		}
	}
	
	private boolean sameName(String a, String b) {
		if (a == null && b == null) {
			return true;
		} else if (a != null && b != null) {
			return a.equals(b);
		}
		return false;
	}

	public boolean hasProblems() {
		return m_inconsistentGroups.size() > 0;
	}
	
	public String getReport() {
		StringBuilder buf = new StringBuilder();
		buf.append(m_inconsistentGroups.size());
		buf.append(" groups of methods related by inheritance and/or interfaces have different deobf names!\n");
		for (Set<MethodEntry> group : m_inconsistentGroups) {
			buf.append("\tGroup with ");
			buf.append(group.size());
			buf.append(" methods:\n");
			for (MethodEntry methodEntry : group) {
				buf.append("\t\t");
				buf.append(methodEntry.toString());
				buf.append(" => ");
				buf.append(m_deobfNamesByObfMethod.get(methodEntry));
				buf.append("\n");
			}
		}
		return buf.toString();
	}
}
