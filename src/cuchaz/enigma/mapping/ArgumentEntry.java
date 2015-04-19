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

import cuchaz.enigma.Util;

public class ArgumentEntry implements Entry, Serializable {
	
	private static final long serialVersionUID = 4472172468162696006L;
	
	private BehaviorEntry m_behaviorEntry;
	private int m_index;
	private String m_name;
	
	public ArgumentEntry(BehaviorEntry behaviorEntry, int index, String name) {
		if (behaviorEntry == null) {
			throw new IllegalArgumentException("Behavior cannot be null!");
		}
		if (index < 0) {
			throw new IllegalArgumentException("Index must be non-negative!");
		}
		if (name == null) {
			throw new IllegalArgumentException("Argument name cannot be null!");
		}
		
		m_behaviorEntry = behaviorEntry;
		m_index = index;
		m_name = name;
	}
	
	public ArgumentEntry(ArgumentEntry other) {
		m_behaviorEntry = (BehaviorEntry)m_behaviorEntry.cloneToNewClass(getClassEntry());
		m_index = other.m_index;
		m_name = other.m_name;
	}
	
	public ArgumentEntry(ArgumentEntry other, String newClassName) {
		m_behaviorEntry = (BehaviorEntry)other.m_behaviorEntry.cloneToNewClass(new ClassEntry(newClassName));
		m_index = other.m_index;
		m_name = other.m_name;
	}
	
	public BehaviorEntry getBehaviorEntry() {
		return m_behaviorEntry;
	}
	
	public int getIndex() {
		return m_index;
	}
	
	@Override
	public String getName() {
		return m_name;
	}
	
	@Override
	public ClassEntry getClassEntry() {
		return m_behaviorEntry.getClassEntry();
	}
	
	@Override
	public String getClassName() {
		return m_behaviorEntry.getClassName();
	}
	
	@Override
	public ArgumentEntry cloneToNewClass(ClassEntry classEntry) {
		return new ArgumentEntry(this, classEntry.getName());
	}
	
	public String getMethodName() {
		return m_behaviorEntry.getName();
	}
	
	public Signature getMethodSignature() {
		return m_behaviorEntry.getSignature();
	}
	
	@Override
	public int hashCode() {
		return Util.combineHashesOrdered(
			m_behaviorEntry,
			Integer.valueOf(m_index).hashCode(),
			m_name.hashCode()
		);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ArgumentEntry) {
			return equals((ArgumentEntry)other);
		}
		return false;
	}
	
	public boolean equals(ArgumentEntry other) {
		return m_behaviorEntry.equals(other.m_behaviorEntry)
			&& m_index == other.m_index
			&& m_name.equals(other.m_name);
	}
	
	@Override
	public String toString() {
		return m_behaviorEntry.toString() + "(" + m_index + ":" + m_name + ")";
	}
}
