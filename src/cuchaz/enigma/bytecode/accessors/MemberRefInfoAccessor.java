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

public class MemberRefInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_classIndex;
	private static Field m_nameAndTypeIndex;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.MemberrefInfo");
			m_classIndex = m_class.getDeclaredField("classIndex");
			m_classIndex.setAccessible(true);
			m_nameAndTypeIndex = m_class.getDeclaredField("nameAndTypeIndex");
			m_nameAndTypeIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isType(ConstInfoAccessor accessor) {
		return m_class.isAssignableFrom(accessor.getItem().getClass());
	}
	
	private Object m_item;
	
	public MemberRefInfoAccessor(Object item) {
		m_item = item;
	}
	
	public int getClassIndex() {
		try {
			return (Integer)m_classIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setClassIndex(int val) {
		try {
			m_classIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public int getNameAndTypeIndex() {
		try {
			return (Integer)m_nameAndTypeIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setNameAndTypeIndex(int val) {
		try {
			m_nameAndTypeIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
