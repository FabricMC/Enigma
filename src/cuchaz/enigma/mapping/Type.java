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
import java.util.Map;

import com.google.common.collect.Maps;

public class Type implements Serializable {
	
	private static final long serialVersionUID = 7862257669347104063L;

	public enum Primitive {
		Byte('B'),
		Character('C'),
		Short('S'),
		Integer('I'),
		Long('J'),
		Float('F'),
		Double('D'),
		Boolean('Z');
		
		private static final Map<Character,Primitive> m_lookup;
		
		static {
			m_lookup = Maps.newTreeMap();
			for (Primitive val : values()) {
				m_lookup.put(val.getCode(), val);
			}
		}
		
		public static Primitive get(char code) {
			return m_lookup.get(code);
		}
		
		private char m_code;
		
		private Primitive(char code) {
			m_code = code;
		}
	
		public char getCode() {
			return m_code;
		}
	}
	
	public static String parseFirst(String in) {
		
		if (in == null || in.length() <= 0) {
			throw new IllegalArgumentException("No type to parse, input is empty!");
		}
		
		// read one type from the input
		
		char c = in.charAt(0);
		
		// first check for void
		if (c == 'V') {
			return "V";
		}
		
		// then check for primitives
		Primitive primitive = Primitive.get(c);
		if (primitive != null) {
			return in.substring(0, 1);
		}
		
		// then check for classes
		if (c == 'L') {
			return readClass(in);
		}
		
		// then check for templates
		if (c == 'T') {
			return readClass(in);
		}

		// then check for arrays
		int dim = countArrayDimension(in);
		if (dim > 0) {
			String arrayType = Type.parseFirst(in.substring(dim));
			return in.substring(0, dim + arrayType.length());
		}
		
		throw new IllegalArgumentException("don't know how to parse: " + in);
	}
	
	protected String m_name;
	
	public Type(String name) {
		
		// don't deal with generics
		// this is just for raw jvm types
		if (name.charAt(0) == 'T' || name.indexOf('<') >= 0 || name.indexOf('>') >= 0) {
			throw new IllegalArgumentException("don't use with generic types or templates: " + name);
		}
		
		m_name = name;
	}
	
	public Type(Type other) {
		m_name = other.m_name;
	}
	
	public Type(ClassEntry classEntry) {
		m_name = "L" + classEntry.getClassName() + ";";
	}
	
	public Type(Type other, ClassNameReplacer replacer) {
		m_name = other.m_name;
		if (other.isClass()) {
			String replacedName = replacer.replace(other.getClassEntry().getClassName());
			if (replacedName != null) {
				m_name = "L" + replacedName + ";";
			}
		} else if (other.isArray() && other.hasClass()) {
			String replacedName = replacer.replace(other.getClassEntry().getClassName());
			if (replacedName != null) {
				m_name = Type.getArrayPrefix(other.getArrayDimension()) + "L" + replacedName + ";";
			}
		}
	}
	
	@Override
	public String toString() {
		return m_name;
	}
	
	public boolean isVoid() {
		return m_name.length() == 1 && m_name.charAt(0) == 'V';
	}
	
	public boolean isPrimitive() {
		return m_name.length() == 1 && Primitive.get(m_name.charAt(0)) != null;
	}
	
	public Primitive getPrimitive() {
		if (!isPrimitive()) {
			throw new IllegalStateException("not a primitive");
		}
		return Primitive.get(m_name.charAt(0));
	}
	
	public boolean isClass() {
		return m_name.charAt(0) == 'L' && m_name.charAt(m_name.length() - 1) == ';';
	}
	
	public ClassEntry getClassEntry() {
		if (isClass()) {
			String name = m_name.substring(1, m_name.length() - 1);
			
			int pos = name.indexOf('<');
			if (pos >= 0) {
				// remove the parameters from the class name
				name = name.substring(0, pos);
			}
			
			return new ClassEntry(name);
			
		} else if (isArray() && getArrayType().isClass()) {
			return getArrayType().getClassEntry();
		} else {
			throw new IllegalStateException("type doesn't have a class");
		}
	}
	
	public boolean isArray() {
		return m_name.charAt(0) == '[';
	}
	
	public int getArrayDimension() {
		if (!isArray()) {
			throw new IllegalStateException("not an array");
		}
		return countArrayDimension(m_name);
	}
	
	public Type getArrayType() {
		if (!isArray()) {
			throw new IllegalStateException("not an array");
		}
		return new Type(m_name.substring(getArrayDimension(), m_name.length()));
	}
	
	private static String getArrayPrefix(int dimension) {
		StringBuilder buf = new StringBuilder();
		for (int i=0; i<dimension; i++) {
			buf.append("[");
		}
		return buf.toString();
	}
	
	public boolean hasClass() {
		return isClass() || (isArray() && getArrayType().hasClass());
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Type) {
			return equals((Type)other);
		}
		return false;
	}
	
	public boolean equals(Type other) {
		return m_name.equals(other.m_name);
	}
	
	public int hashCode() {
		return m_name.hashCode();
	}
	
	private static int countArrayDimension(String in) {
		int i=0;
		for(; i < in.length() && in.charAt(i) == '['; i++);
		return i;
	}
	
	private static String readClass(String in) {
		// read all the characters in the buffer until we hit a ';'
		// include the parameters too
		StringBuilder buf = new StringBuilder();
		int depth = 0;
		for (int i=0; i<in.length(); i++) {
			char c = in.charAt(i);
			buf.append(c);
			
			if (c == '<') {
				depth++;
			} else if (c == '>') {
				depth--;
			} else if (depth == 0 && c == ';') {
				return buf.toString();
			}
		}
		return null;
	}
}
