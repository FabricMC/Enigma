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
import java.util.List;

import com.google.common.collect.Lists;

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
		
		if (isInnerClass() && getInnermostClassName().indexOf('/') >= 0) {
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
	
	public List<String> getClassChainNames() {
		return Lists.newArrayList(m_name.split("\\$"));
	}
	
	public List<ClassEntry> getClassChain() {
		List<ClassEntry> entries = Lists.newArrayList();
		StringBuilder buf = new StringBuilder();
		for (String name : getClassChainNames()) {
			if (buf.length() > 0) {
				buf.append("$");
			}
			buf.append(name);
			entries.add(new ClassEntry(buf.toString()));
		}
		return entries;
	}
	
	public String getOutermostClassName() {
		if (isInnerClass()) {
			return m_name.substring(0, m_name.indexOf('$'));
		}
		return m_name;
	}
	
	public ClassEntry getOutermostClassEntry() {
		return new ClassEntry(getOutermostClassName());
	}
	
	public String getOuterClassName() {
		if (!isInnerClass()) {
			throw new Error("This is not an inner class!");
		}
		return m_name.substring(0, m_name.lastIndexOf('$'));
	}
	
	public ClassEntry getOuterClassEntry() {
		return new ClassEntry(getOuterClassName());
	}
	
	public String getInnermostClassName() {
		if (!isInnerClass()) {
			throw new Error("This is not an inner class!");
		}
		return m_name.substring(m_name.lastIndexOf('$') + 1);
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
		int pos = m_name.lastIndexOf('/');
		if (pos > 0) {
			return m_name.substring(pos + 1);
		}
		return m_name;
	}
	
	public ClassEntry buildClassEntry(List<ClassEntry> classChain) {
		assert(classChain.contains(this));
		StringBuilder buf = new StringBuilder();
		for (ClassEntry chainEntry : classChain) {
			if (buf.length() == 0) {
				buf.append(chainEntry.getName());
			} else {
				buf.append("$");
				buf.append(chainEntry.isInnerClass() ? chainEntry.getInnermostClassName() : chainEntry.getSimpleName());
			}
			
			if (chainEntry == this) {
				break;
			}
		}
		return new ClassEntry(buf.toString());
	}
}
