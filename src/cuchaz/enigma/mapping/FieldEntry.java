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

public class FieldEntry implements Entry, Serializable {
	
	private static final long serialVersionUID = 3004663582802885451L;
	
	private ClassEntry m_classEntry;
	private String m_name;
	private Type m_type;
	
	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public FieldEntry(ClassEntry classEntry, String name, Type type) {
		if (classEntry == null) {
			throw new IllegalArgumentException("Class cannot be null!");
		}
		if (name == null) {
			throw new IllegalArgumentException("Field name cannot be null!");
		}
		if (type == null) {
			throw new IllegalArgumentException("Field type cannot be null!");
		}
		
		m_classEntry = classEntry;
		m_name = name;
		m_type = type;
	}
	
	public FieldEntry(FieldEntry other) {
		this(other, new ClassEntry(other.m_classEntry));
	}
	
	public FieldEntry(FieldEntry other, ClassEntry newClassEntry) {
		m_classEntry = newClassEntry;
		m_name = other.m_name;
		m_type = other.m_type;
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
	public String getClassName() {
		return m_classEntry.getName();
	}
	
	public Type getType() {
		return m_type;
	}
	
	@Override
	public FieldEntry cloneToNewClass(ClassEntry classEntry) {
		return new FieldEntry(this, classEntry);
	}
	
	@Override
	public int hashCode() {
		return Util.combineHashesOrdered(m_classEntry, m_name, m_type);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof FieldEntry) {
			return equals((FieldEntry)other);
		}
		return false;
	}
	
	public boolean equals(FieldEntry other) {
		return m_classEntry.equals(other.m_classEntry)
			&& m_name.equals(other.m_name)
			&& m_type.equals(other.m_type);
	}
	
	@Override
	public String toString() {
		return m_classEntry.getName() + "." + m_name + ":" + m_type;
	}
}
