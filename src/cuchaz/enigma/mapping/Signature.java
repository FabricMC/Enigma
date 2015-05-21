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
import java.util.List;

import com.google.common.collect.Lists;

import cuchaz.enigma.Util;

public class Signature implements Serializable {
	
	private static final long serialVersionUID = -5843719505729497539L;
	
	private List<Type> m_argumentTypes;
	private Type m_returnType;
	
	public Signature(String signature) {
		try {
			m_argumentTypes = Lists.newArrayList();
			int i=0;
			while (i<signature.length()) {
				char c = signature.charAt(i);
				if (c == '(') {
					assert(m_argumentTypes.isEmpty());
					assert(m_returnType == null);
					i++;
				} else if (c == ')') {
					i++;
					break;
				} else {
					String type = Type.parseFirst(signature.substring(i));
					m_argumentTypes.add(new Type(type));
					i += type.length();
				}
			}
			m_returnType = new Type(Type.parseFirst(signature.substring(i)));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to parse signature: " + signature, ex);
		}
	}
	
	public Signature(Signature other) {
		m_argumentTypes = Lists.newArrayList(other.m_argumentTypes);
		m_returnType = new Type(other.m_returnType);
	}
	
	public Signature(Signature other, ClassNameReplacer replacer) {
		m_argumentTypes = Lists.newArrayList(other.m_argumentTypes);
		for (int i=0; i<m_argumentTypes.size(); i++) {
			m_argumentTypes.set(i, new Type(m_argumentTypes.get(i), replacer));
		}
		m_returnType = new Type(other.m_returnType, replacer);
	}
	
	public List<Type> getArgumentTypes() {
		return m_argumentTypes;
	}
	
	public Type getReturnType() {
		return m_returnType;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		for (Type type : m_argumentTypes) {
			buf.append(type.toString());
		}
		buf.append(")");
		buf.append(m_returnType.toString());
		return buf.toString();
	}
	
	public Iterable<Type> types() {
		List<Type> types = Lists.newArrayList();
		types.addAll(m_argumentTypes);
		types.add(m_returnType);
		return types;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Signature) {
			return equals((Signature)other);
		}
		return false;
	}
	
	public boolean equals(Signature other) {
		return m_argumentTypes.equals(other.m_argumentTypes) && m_returnType.equals(other.m_returnType);
	}
	
	@Override
	public int hashCode() {
		return Util.combineHashesOrdered(m_argumentTypes.hashCode(), m_returnType.hashCode());
	}

	public boolean hasClass(ClassEntry classEntry) {
		for (Type type : types()) {
			if (type.hasClass() && type.getClassEntry().equals(classEntry)) {
				return true;
			}
		}
		return false;
	}
}
