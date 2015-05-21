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

public class StringInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_stringIndex;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.StringInfo");
			m_stringIndex = m_class.getDeclaredField("string");
			m_stringIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isType(ConstInfoAccessor accessor) {
		return m_class.isAssignableFrom(accessor.getItem().getClass());
	}
	
	private Object m_item;
	
	public StringInfoAccessor(Object item) {
		m_item = item;
	}
	
	public int getStringIndex() {
		try {
			return (Integer)m_stringIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setStringIndex(int val) {
		try {
			m_stringIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
