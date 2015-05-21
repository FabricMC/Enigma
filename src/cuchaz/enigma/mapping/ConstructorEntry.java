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

public class ConstructorEntry implements BehaviorEntry, Serializable {
	
	private static final long serialVersionUID = -868346075317366758L;
	
	private ClassEntry m_classEntry;
	private Signature m_signature;
	
	public ConstructorEntry(ClassEntry classEntry) {
		this(classEntry, null);
	}
	
	public ConstructorEntry(ClassEntry classEntry, Signature signature) {
		if (classEntry == null) {
			throw new IllegalArgumentException("Class cannot be null!");
		}
		
		m_classEntry = classEntry;
		m_signature = signature;
	}
	
	public ConstructorEntry(ConstructorEntry other) {
		m_classEntry = new ClassEntry(other.m_classEntry);
		m_signature = other.m_signature;
	}
	
	public ConstructorEntry(ConstructorEntry other, String newClassName) {
		m_classEntry = new ClassEntry(newClassName);
		m_signature = other.m_signature;
	}
	
	@Override
	public ClassEntry getClassEntry() {
		return m_classEntry;
	}
	
	@Override
	public String getName() {
		if (isStatic()) {
			return "<clinit>";
		}
		return "<init>";
	}
	
	public boolean isStatic() {
		return m_signature == null;
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
	public ConstructorEntry cloneToNewClass(ClassEntry classEntry) {
		return new ConstructorEntry(this, classEntry.getName());
	}
	
	@Override
	public int hashCode() {
		if (isStatic()) {
			return Util.combineHashesOrdered(m_classEntry);
		} else {
			return Util.combineHashesOrdered(m_classEntry, m_signature);
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ConstructorEntry) {
			return equals((ConstructorEntry)other);
		}
		return false;
	}
	
	public boolean equals(ConstructorEntry other) {
		if (isStatic() != other.isStatic()) {
			return false;
		}
		
		if (isStatic()) {
			return m_classEntry.equals(other.m_classEntry);
		} else {
			return m_classEntry.equals(other.m_classEntry) && m_signature.equals(other.m_signature);
		}
	}
	
	@Override
	public String toString() {
		if (isStatic()) {
			return m_classEntry.getName() + "." + getName();
		} else {
			return m_classEntry.getName() + "." + getName() + m_signature;
		}
	}
}
