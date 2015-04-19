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
package cuchaz.enigma.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import cuchaz.enigma.bytecode.accessors.ClassInfoAccessor;
import cuchaz.enigma.bytecode.accessors.ConstInfoAccessor;
import cuchaz.enigma.bytecode.accessors.MemberRefInfoAccessor;

public class ConstPoolEditor {
	
	private static Method m_getItem;
	private static Method m_addItem;
	private static Method m_addItem0;
	private static Field m_items;
	private static Field m_cache;
	private static Field m_numItems;
	private static Field m_objects;
	private static Field m_elements;
	private static Method m_methodWritePool;
	private static Constructor<ConstPool> m_constructorPool;
	
	static {
		try {
			m_getItem = ConstPool.class.getDeclaredMethod("getItem", int.class);
			m_getItem.setAccessible(true);
			
			m_addItem = ConstPool.class.getDeclaredMethod("addItem", Class.forName("javassist.bytecode.ConstInfo"));
			m_addItem.setAccessible(true);
			
			m_addItem0 = ConstPool.class.getDeclaredMethod("addItem0", Class.forName("javassist.bytecode.ConstInfo"));
			m_addItem0.setAccessible(true);
			
			m_items = ConstPool.class.getDeclaredField("items");
			m_items.setAccessible(true);
			
			m_cache = ConstPool.class.getDeclaredField("itemsCache");
			m_cache.setAccessible(true);
			
			m_numItems = ConstPool.class.getDeclaredField("numOfItems");
			m_numItems.setAccessible(true);
			
			m_objects = Class.forName("javassist.bytecode.LongVector").getDeclaredField("objects");
			m_objects.setAccessible(true);
			
			m_elements = Class.forName("javassist.bytecode.LongVector").getDeclaredField("elements");
			m_elements.setAccessible(true);
			
			m_methodWritePool = ConstPool.class.getDeclaredMethod("write", DataOutputStream.class);
			m_methodWritePool.setAccessible(true);
			
			m_constructorPool = ConstPool.class.getDeclaredConstructor(DataInputStream.class);
			m_constructorPool.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	private ConstPool m_pool;
	
	public ConstPoolEditor(ConstPool pool) {
		m_pool = pool;
	}
	
	public void writePool(DataOutputStream out) {
		try {
			m_methodWritePool.invoke(m_pool, out);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static ConstPool readPool(DataInputStream in) {
		try {
			return m_constructorPool.newInstance(in);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public String getMemberrefClassname(int memberrefIndex) {
		return Descriptor.toJvmName(m_pool.getClassInfo(m_pool.getMemberClass(memberrefIndex)));
	}
	
	public String getMemberrefName(int memberrefIndex) {
		return m_pool.getUtf8Info(m_pool.getNameAndTypeName(m_pool.getMemberNameAndType(memberrefIndex)));
	}
	
	public String getMemberrefType(int memberrefIndex) {
		return m_pool.getUtf8Info(m_pool.getNameAndTypeDescriptor(m_pool.getMemberNameAndType(memberrefIndex)));
	}
	
	public ConstInfoAccessor getItem(int index) {
		try {
			Object entry = m_getItem.invoke(m_pool, index);
			if (entry == null) {
				return null;
			}
			return new ConstInfoAccessor(entry);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public int addItem(Object item) {
		try {
			return (Integer)m_addItem.invoke(m_pool, item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public int addItemForceNew(Object item) {
		try {
			return (Integer)m_addItem0.invoke(m_pool, item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void removeLastItem() {
		try {
			// remove the item from the cache
			HashMap cache = getCache();
			if (cache != null) {
				Object item = getItem(m_pool.getSize() - 1);
				cache.remove(item);
			}
			
			// remove the actual item
			// based off of LongVector.addElement()
			Object items = m_items.get(m_pool);
			Object[][] objects = (Object[][])m_objects.get(items);
			int numElements = (Integer)m_elements.get(items) - 1;
			int nth = numElements >> 7;
			int offset = numElements & (128 - 1);
			objects[nth][offset] = null;
			
			// decrement the number of items
			m_elements.set(items, numElements);
			m_numItems.set(m_pool, (Integer)m_numItems.get(m_pool) - 1);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public HashMap getCache() {
		try {
			return (HashMap)m_cache.get(m_pool);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void changeMemberrefNameAndType(int memberrefIndex, String newName, String newType) {
		// NOTE: when changing values, we always need to copy-on-write
		try {
			// get the memberref item
			Object item = getItem(memberrefIndex).getItem();
			
			// update the cache
			HashMap cache = getCache();
			if (cache != null) {
				cache.remove(item);
			}
			
			new MemberRefInfoAccessor(item).setNameAndTypeIndex(m_pool.addNameAndTypeInfo(newName, newType));
			
			// update the cache
			if (cache != null) {
				cache.put(item, item);
			}
		} catch (Exception ex) {
			throw new Error(ex);
		}
		
		// make sure the change worked
		assert (newName.equals(getMemberrefName(memberrefIndex)));
		assert (newType.equals(getMemberrefType(memberrefIndex)));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void changeClassName(int classNameIndex, String newName) {
		// NOTE: when changing values, we always need to copy-on-write
		try {
			// get the class item
			Object item = getItem(classNameIndex).getItem();
			
			// update the cache
			HashMap cache = getCache();
			if (cache != null) {
				cache.remove(item);
			}
			
			// add the new name and repoint the name-and-type to it
			new ClassInfoAccessor(item).setNameIndex(m_pool.addUtf8Info(newName));
			
			// update the cache
			if (cache != null) {
				cache.put(item, item);
			}
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static ConstPool newConstPool() {
		// const pool expects the name of a class to initialize itself
		// but we want an empty pool
		// so give it a bogus name, and then clear the entries afterwards
		ConstPool pool = new ConstPool("a");
		
		ConstPoolEditor editor = new ConstPoolEditor(pool);
		int size = pool.getSize();
		for (int i = 0; i < size - 1; i++) {
			editor.removeLastItem();
		}
		
		// make sure the pool is actually empty
		// although, in this case "empty" means one thing in it
		// the JVM spec says index 0 should be reserved
		assert (pool.getSize() == 1);
		assert (editor.getItem(0) == null);
		assert (editor.getItem(1) == null);
		assert (editor.getItem(2) == null);
		assert (editor.getItem(3) == null);
		
		// also, clear the cache
		editor.getCache().clear();
		
		return pool;
	}
	
	public String dump() {
		StringBuilder buf = new StringBuilder();
		for (int i = 1; i < m_pool.getSize(); i++) {
			buf.append(String.format("%4d", i));
			buf.append("   ");
			buf.append(getItem(i).toString());
			buf.append("\n");
		}
		return buf.toString();
	}
}
