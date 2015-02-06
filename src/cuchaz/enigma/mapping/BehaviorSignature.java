package cuchaz.enigma.mapping;

import java.util.List;

import com.beust.jcommander.internal.Lists;

public class BehaviorSignature {
	
	public static interface ClassReplacer {
		ClassEntry replace(ClassEntry entry);
	}
	
	private List<Type> m_argumentTypes;
	private Type m_returnType;
	
	public BehaviorSignature(String signature) {
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
	}
	
	public BehaviorSignature(BehaviorSignature other, ClassReplacer replacer) {
		m_argumentTypes = Lists.newArrayList(other.m_argumentTypes);
		for (int i=0; i<m_argumentTypes.size(); i++) {
			Type type = m_argumentTypes.get(i);
			if (type.isClass()) {
				ClassEntry newClassEntry = replacer.replace(type.getClassEntry());
				if (newClassEntry != null) {
					m_argumentTypes.set(i, new Type(newClassEntry));
				}
			}
		}
		m_returnType = other.m_returnType;
		if (other.m_returnType.isClass()) {
			ClassEntry newClassEntry = replacer.replace(m_returnType.getClassEntry());
			if (newClassEntry != null) {
				m_returnType = new Type(newClassEntry);
			}
		}
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
		for (int i=0; i<m_argumentTypes.size(); i++) {
			if (i > 0) {
				buf.append(",");
			}
			buf.append(m_argumentTypes.get(i).toString());
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
	
	public Iterable<ClassEntry> classes() {
		List<ClassEntry> out = Lists.newArrayList();
		for (Type type : types()) {
			if (type.isClass()) {
				out.add(type.getClassEntry());
			}
		}
		return out;
	}
}
