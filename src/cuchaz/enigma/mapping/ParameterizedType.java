package cuchaz.enigma.mapping;

import cuchaz.enigma.Util;



public class ParameterizedType extends Type {

	private static final long serialVersionUID = 1758975507937309011L;

	public ParameterizedType(Type other) {
		super(other);
		for (int i=0; i<m_parameters.size(); i++) {
			m_parameters.set(i, new ParameterizedType(m_parameters.get(i)));
		}
	}
	
	public ParameterizedType(ParameterizedType type, ClassNameReplacer replacer) {
		this(new Type(type, replacer));
	}

	@Override
	public String toString() {
		if (hasParameters()) {
			StringBuilder buf = new StringBuilder();
			buf.append(m_name.substring(0, m_name.length() - 1));
			buf.append("<");
			for (Type parameter : parameters()) {
				buf.append(parameter.toString());
			}
			buf.append(">;");
			return buf.toString();
		} else {
			return m_name;
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ParameterizedType) {
			return equals((ParameterizedType)other);
		}
		return false;
	}
	
	public boolean equals(ParameterizedType other) {
		return m_name.equals(other.m_name) && m_parameters.equals(other.m_parameters);
	}
	
	public int hashCode() {
		return Util.combineHashesOrdered(m_name.hashCode(), m_parameters.hashCode());
	}
	
}
