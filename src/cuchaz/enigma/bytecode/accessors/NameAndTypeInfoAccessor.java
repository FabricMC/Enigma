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
package cuchaz.enigma.bytecode.accessors;

import java.lang.reflect.Field;

public class NameAndTypeInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_nameIndex;
	private static Field m_typeIndex;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.NameAndTypeInfo");
			m_nameIndex = m_class.getDeclaredField("memberName");
			m_nameIndex.setAccessible(true);
			m_typeIndex = m_class.getDeclaredField("typeDescriptor");
			m_typeIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isType(ConstInfoAccessor accessor) {
		return m_class.isAssignableFrom(accessor.getItem().getClass());
	}
	
	private Object m_item;
	
	public NameAndTypeInfoAccessor(Object item) {
		m_item = item;
	}
	
	public int getNameIndex() {
		try {
			return (Integer)m_nameIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setNameIndex(int val) {
		try {
			m_nameIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public int getTypeIndex() {
		try {
			return (Integer)m_typeIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setTypeIndex(int val) {
		try {
			m_typeIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
