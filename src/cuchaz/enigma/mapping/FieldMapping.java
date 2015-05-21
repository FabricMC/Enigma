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

public class FieldMapping implements Serializable, Comparable<FieldMapping>, MemberMapping<FieldEntry> {
	
	private static final long serialVersionUID = 8610742471440861315L;
	
	private String m_obfName;
	private String m_deobfName;
	private Type m_obfType;
	
	public FieldMapping(String obfName, Type obfType, String deobfName) {
		m_obfName = obfName;
		m_deobfName = NameValidator.validateFieldName(deobfName);
		m_obfType = obfType;
	}
	
	public FieldMapping(FieldMapping other, ClassNameReplacer obfClassNameReplacer) {
		m_obfName = other.m_obfName;
		m_deobfName = other.m_deobfName;
		m_obfType = new Type(other.m_obfType, obfClassNameReplacer);
	}

	@Override
	public String getObfName() {
		return m_obfName;
	}
	
	public void setObfName(String val) {
		m_obfName = NameValidator.validateFieldName(val);
	}
	
	public String getDeobfName() {
		return m_deobfName;
	}
	
	public void setDeobfName(String val) {
		m_deobfName = NameValidator.validateFieldName(val);
	}
	
	public Type getObfType() {
		return m_obfType;
	}
	
	public void setObfType(Type val) {
		m_obfType = val;
	}
	
	@Override
	public int compareTo(FieldMapping other) {
		return (m_obfName + m_obfType).compareTo(other.m_obfName + other.m_obfType);
	}

	public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {
		
		// rename obf classes in the type
		Type newType = new Type(m_obfType, new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				if (className.equals(oldObfClassName)) {
					return newObfClassName;
				}
				return null;
			}
		});
		
		if (!newType.equals(m_obfType)) {
			m_obfType = newType;
			return true;
		}
		return false;
	}

	@Override
	public FieldEntry getObfEntry(ClassEntry classEntry) {
		return new FieldEntry(classEntry, m_obfName, new Type(m_obfType));
	}
}
