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

public class MethodEntry implements BehaviorEntry, Serializable {
	
	private static final long serialVersionUID = 4770915224467247458L;
	
	private ClassEntry m_classEntry;
	private String m_name;
	private Signature m_signature;
	
	public MethodEntry(ClassEntry classEntry, String name, Signature signature) {
		if (classEntry == null) {
			throw new IllegalArgumentException("Class cannot be null!");
		}
		if (name == null) {
			throw new IllegalArgumentException("Method name cannot be null!");
		}
		if (signature == null) {
			throw new IllegalArgumentException("Method signature cannot be null!");
		}
		if (name.startsWith("<")) {
			throw new IllegalArgumentException("Don't use MethodEntry for a constructor!");
		}
		
		m_classEntry = classEntry;
		m_name = name;
		m_signature = signature;
	}
	
	public MethodEntry(MethodEntry other) {
		m_classEntry = new ClassEntry(other.m_classEntry);
		m_name = other.m_name;
		m_signature = other.m_signature;
	}
	
	public MethodEntry(MethodEntry other, String newClassName) {
		m_classEntry = new ClassEntry(newClassName);
		m_name = other.m_name;
		m_signature = other.m_signature;
	}
	
	@Override
	public ClassEntry getClassEntry() {
		return m_classEntry;
	}
	
	@Override
	public String getName() {
		return m_name;
	}
	
	@Override
	public Signature getSignature() {
		return m_signature;
	}
	
	@Override
	public String getClassName() {
		return m_classEntry.getName();
	}
	
	@Override
	public MethodEntry cloneToNewClass(ClassEntry classEntry) {
		return new MethodEntry(this, classEntry.getName());
	}
	
	@Override
	public int hashCode() {
		return Util.combineHashesOrdered(m_classEntry, m_name, m_signature);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof MethodEntry) {
			return equals((MethodEntry)other);
		}
		return false;
	}
	
	public boolean equals(MethodEntry other) {
		return m_classEntry.equals(other.m_classEntry)
			&& m_name.equals(other.m_name)
			&& m_signature.equals(other.m_signature);
	}
	
	@Override
	public String toString() {
		return m_classEntry.getName() + "." + m_name + m_signature;
	}
}
