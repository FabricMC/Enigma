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

import java.io.Serializable;

public class ClassEntry implements Entry, Serializable {
	
	private static final long serialVersionUID = 4235460580973955811L;
	
	private String m_name;
	
	public ClassEntry(String className) {
		if (className == null) {
			throw new IllegalArgumentException("Class name cannot be null!");
		}
		if (className.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name must be in JVM format. ie, path/to/package/class$inner : " + className);
		}
		
		m_name = className;
		
		if (isInnerClass() && getInnerClassName().indexOf('/') >= 0) {
			throw new IllegalArgumentException("Inner class must not have a package: " + className);
		}
	}
	
	public ClassEntry(ClassEntry other) {
		m_name = other.m_name;
	}
	
	@Override
	public String getName() {
		return m_name;
	}
	
	@Override
	public String getClassName() {
		return m_name;
	}
	
	@Override
	public ClassEntry getClassEntry() {
		return this;
	}
	
	@Override
	public ClassEntry cloneToNewClass(ClassEntry classEntry) {
		return classEntry;
	}
	
	@Override
	public int hashCode() {
		return m_name.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassEntry) {
			return equals((ClassEntry)other);
		}
		return false;
	}
	
	public boolean equals(ClassEntry other) {
		return m_name.equals(other.m_name);
	}
	
	@Override
	public String toString() {
		return m_name;
	}
	
	public boolean isInnerClass() {
		return m_name.lastIndexOf('$') >= 0;
	}
	
	public String getOuterClassName() {
		if (isInnerClass()) {
			return m_name.substring(0, m_name.lastIndexOf('$'));
		}
		return m_name;
	}
	
	public String getInnerClassName() {
		if (!isInnerClass()) {
			throw new Error("This is not an inner class!");
		}
		return m_name.substring(m_name.lastIndexOf('$') + 1);
	}
	
	public ClassEntry getOuterClassEntry() {
		return new ClassEntry(getOuterClassName());
	}
	
	public boolean isInDefaultPackage() {
		return m_name.indexOf('/') < 0;
	}
	
	public String getPackageName() {
		int pos = m_name.lastIndexOf('/');
		if (pos > 0) {
			return m_name.substring(0, pos);
		}
		return null;
	}
	
	public String getSimpleName() {
		if (isInnerClass()) {
			return getInnerClassName();
		}
		int pos = m_name.lastIndexOf('/');
		if (pos > 0) {
			return m_name.substring(pos + 1);
		}
		return m_name;
	}
}
