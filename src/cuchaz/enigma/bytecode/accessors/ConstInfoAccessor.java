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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cuchaz.enigma.bytecode.InfoType;

public class ConstInfoAccessor {
	
	private static Class<?> m_class;
	private static Field m_index;
	private static Method m_getTag;
	
	static {
		try {
			m_class = Class.forName("javassist.bytecode.ConstInfo");
			m_index = m_class.getDeclaredField("index");
			m_index.setAccessible(true);
			m_getTag = m_class.getMethod("getTag");
			m_getTag.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	private Object m_item;
	
	public ConstInfoAccessor(Object item) {
		if (item == null) {
			throw new IllegalArgumentException("item cannot be null!");
		}
		m_item = item;
	}
	
	public ConstInfoAccessor(DataInputStream in) throws IOException {
		try {
			// read the entry
			String className = in.readUTF();
			int oldIndex = in.readInt();
			
			// NOTE: ConstInfo instances write a type id (a "tag"), but they don't read it back
			// so we have to read it here
			in.readByte();
			
			Constructor<?> constructor = Class.forName(className).getConstructor(DataInputStream.class, int.class);
			constructor.setAccessible(true);
			m_item = constructor.newInstance(in, oldIndex);
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public Object getItem() {
		return m_item;
	}
	
	public int getIndex() {
		try {
			return (Integer)m_index.get(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void setIndex(int val) {
		try {
			m_index.set(m_item, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public int getTag() {
		try {
			return (Integer)m_getTag.invoke(m_item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public ConstInfoAccessor copy() {
		return new ConstInfoAccessor(copyItem());
	}
	
	public Object copyItem() {
		// I don't know of a simpler way to copy one of these silly things...
		try {
			// serialize the item
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);
			write(out);
			
			// deserialize the item
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
			Object item = new ConstInfoAccessor(in).getItem();
			in.close();
			
			return item;
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public void write(DataOutputStream out) throws IOException {
		try {
			out.writeUTF(m_item.getClass().getName());
			out.writeInt(getIndex());
			
			Method method = m_item.getClass().getMethod("write", DataOutputStream.class);
			method.setAccessible(true);
			method.invoke(m_item, out);
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@Override
	public String toString() {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			PrintWriter out = new PrintWriter(buf);
			Method print = m_item.getClass().getMethod("print", PrintWriter.class);
			print.setAccessible(true);
			print.invoke(m_item, out);
			out.close();
			return buf.toString().replace("\n", "");
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public InfoType getType() {
		return InfoType.getByTag(getTag());
	}
}
