/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.mapping;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cuchaz.enigma.mapping.entry.ClassEntry;

import java.util.Map;
import java.util.function.Function;

public class TypeDescriptor {

	protected final String desc;

	public TypeDescriptor(String desc) {
		Preconditions.checkNotNull(desc, "Desc cannot be null");

		// don't deal with generics
		// this is just for raw jvm types
		if (desc.charAt(0) == 'T' || desc.indexOf('<') >= 0 || desc.indexOf('>') >= 0) {
			throw new IllegalArgumentException("don't use with generic types or templates: " + desc);
		}

		this.desc = desc;
	}

	public static String parseFirst(String in) {

		if (in == null || in.length() <= 0) {
			throw new IllegalArgumentException("No desc to parse, input is empty!");
		}

		// read one desc from the input

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
			String arrayType = TypeDescriptor.parseFirst(in.substring(dim));
			return in.substring(0, dim + arrayType.length());
		}

		throw new IllegalArgumentException("don't know how to parse: " + in);
	}

	private static int countArrayDimension(String in) {
		int i = 0;
		while (i < in.length() && in.charAt(i) == '[')
			i++;
		return i;
	}

	private static String readClass(String in) {
		// read all the characters in the buffer until we hit a ';'
		// include the parameters too
		StringBuilder buf = new StringBuilder();
		int depth = 0;
		for (int i = 0; i < in.length(); i++) {
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

	public static TypeDescriptor of(String name) {
		return new TypeDescriptor("L" + name + ";");
	}

	@Override
	public String toString() {
		return this.desc;
	}

	public boolean isVoid() {
		return this.desc.length() == 1 && this.desc.charAt(0) == 'V';
	}

	public boolean isPrimitive() {
		return this.desc.length() == 1 && Primitive.get(this.desc.charAt(0)) != null;
	}

	public Primitive getPrimitive() {
		if (!isPrimitive()) {
			throw new IllegalStateException("not a primitive");
		}
		return Primitive.get(this.desc.charAt(0));
	}

	public boolean isType() {
		return this.desc.charAt(0) == 'L' && this.desc.charAt(this.desc.length() - 1) == ';';
	}

	public ClassEntry getTypeEntry() {
		if (isType()) {
			String name = this.desc.substring(1, this.desc.length() - 1);

			int pos = name.indexOf('<');
			if (pos >= 0) {
				// remove the parameters from the class name
				name = name.substring(0, pos);
			}

			return new ClassEntry(name);

		} else if (isArray() && getArrayType().isType()) {
			return getArrayType().getTypeEntry();
		} else {
			throw new IllegalStateException("desc doesn't have a class");
		}
	}

	public boolean isArray() {
		return this.desc.charAt(0) == '[';
	}

	public int getArrayDimension() {
		if (!isArray()) {
			throw new IllegalStateException("not an array");
		}
		return countArrayDimension(this.desc);
	}

	public TypeDescriptor getArrayType() {
		if (!isArray()) {
			throw new IllegalStateException("not an array");
		}
		return new TypeDescriptor(this.desc.substring(getArrayDimension(), this.desc.length()));
	}

	public boolean containsType() {
		return isType() || (isArray() && getArrayType().containsType());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TypeDescriptor && equals((TypeDescriptor) other);
	}

	public boolean equals(TypeDescriptor other) {
		return this.desc.equals(other.desc);
	}

	@Override
	public int hashCode() {
		return this.desc.hashCode();
	}

	public TypeDescriptor remap(Function<String, String> remapper) {
		String desc = this.desc;
		if (isType() || (isArray() && containsType())) {
			String replacedName = remapper.apply(this.getTypeEntry().getName());
			if (replacedName != null) {
				if (this.isType()) {
					desc = "L" + replacedName + ";";
				} else {
					desc = getArrayPrefix(this.getArrayDimension()) + "L" + replacedName + ";";
				}
			}
		}
		return new TypeDescriptor(desc);
	}

	private static String getArrayPrefix(int dimension) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < dimension; i++) {
			buf.append("[");
		}
		return buf.toString();
	}

	public int getSize() {
		switch (desc.charAt(0)) {
			case 'J':
			case 'D':
				if (desc.length() == 1) {
					return 2;
				} else {
					return 1;
				}
			default:
				return 1;
		}
	}

	public enum Primitive {
		Byte('B'),
		Character('C'),
		Short('S'),
		Integer('I'),
		Long('J'),
		Float('F'),
		Double('D'),
		Boolean('Z');

		private static final Map<Character, Primitive> lookup;

		static {
			lookup = Maps.newTreeMap();
			for (Primitive val : values()) {
				lookup.put(val.getCode(), val);
			}
		}

		private char code;

		Primitive(char code) {
			this.code = code;
		}

		public static Primitive get(char code) {
			return lookup.get(code);
		}

		public char getCode() {
			return this.code;
		}
	}
}
