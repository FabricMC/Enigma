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

public class InvokeDynamicInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_bootstrapIndex;
	private static Field m_nameAndTypeIndex;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.InvokeDynamicInfo");
			m_bootstrapIndex = m_class.getDeclaredField("bootstrap");
			m_bootstrapIndex.setAccessible(true);
			m_nameAndTypeIndex = m_class.getDeclaredField("nameAndType");
			m_nameAndTypeIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isType(ConstInfoAccessor accessor) {
		return m_class.isAssignableFrom(accessor.getItem().getClass());
	}
	
	private Object m_item;
	
	public InvokeDynamicInfoAccessor(Object item) {
		m_item = item;
	}
	
	public int getBootstrapIndex() {
		try {
			return (Integer)m_bootstrapIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setBootstrapIndex(int val) {
		try {
			m_bootstrapIndex.set(m_item, val);
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
