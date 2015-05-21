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

public class ClassInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_nameIndex;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.ClassInfo");
			m_nameIndex = m_class.getDeclaredField("name");
			m_nameIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isType(ConstInfoAccessor accessor) {
		return m_class.isAssignableFrom(accessor.getItem().getClass());
	}
	
	private Object m_item;
	
	public ClassInfoAccessor(Object item) {
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
}
