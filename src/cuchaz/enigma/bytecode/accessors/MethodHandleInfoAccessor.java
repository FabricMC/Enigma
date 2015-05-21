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

public class MethodHandleInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_kindIndex;
	private static Field m_indexIndex;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.MethodHandleInfo");
			m_kindIndex = m_class.getDeclaredField("refKind");
			m_kindIndex.setAccessible(true);
			m_indexIndex = m_class.getDeclaredField("refIndex");
			m_indexIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isType(ConstInfoAccessor accessor) {
		return m_class.isAssignableFrom(accessor.getItem().getClass());
	}
	
	private Object m_item;
	
	public MethodHandleInfoAccessor(Object item) {
		m_item = item;
	}
	
	public int getTypeIndex() {
		try {
			return (Integer)m_kindIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setTypeIndex(int val) {
		try {
			m_kindIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public int getMethodRefIndex() {
		try {
			return (Integer)m_indexIndex.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setMethodRefIndex(int val) {
		try {
			m_indexIndex.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
