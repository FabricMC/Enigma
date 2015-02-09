/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class MethodMapping implements Serializable, Comparable<MethodMapping> {
	
	private static final long serialVersionUID = -4409570216084263978L;
	
	private String m_obfName;
	private String m_deobfName;
	private Signature m_obfSignature;
	private Map<Integer,ArgumentMapping> m_arguments;
	
	public MethodMapping(String obfName, Signature obfSignature) {
		this(obfName, obfSignature, null);
	}
	
	public MethodMapping(String obfName, Signature obfSignature, String deobfName) {
		if (obfName == null) {
			throw new IllegalArgumentException("obf name cannot be null!");
		}
		if (obfSignature == null) {
			throw new IllegalArgumentException("obf signature cannot be null!");
		}
		m_obfName = obfName;
		m_deobfName = NameValidator.validateMethodName(deobfName);
		m_obfSignature = obfSignature;
		m_arguments = new TreeMap<Integer,ArgumentMapping>();
	}
	
	public String getObfName() {
		return m_obfName;
	}
	
	public String getDeobfName() {
		return m_deobfName;
	}
	
	public void setDeobfName(String val) {
		m_deobfName = NameValidator.validateMethodName(val);
	}
	
	public Signature getObfSignature() {
		return m_obfSignature;
	}
	
	public Iterable<ArgumentMapping> arguments() {
		return m_arguments.values();
	}
	
	public boolean isConstructor() {
		return m_obfName.startsWith("<");
	}
	
	public void addArgumentMapping(ArgumentMapping argumentMapping) {
		boolean wasAdded = m_arguments.put(argumentMapping.getIndex(), argumentMapping) == null;
		assert (wasAdded);
	}
	
	public String getObfArgumentName(int index) {
		ArgumentMapping argumentMapping = m_arguments.get(index);
		if (argumentMapping != null) {
			return argumentMapping.getName();
		}
		
		return null;
	}
	
	public String getDeobfArgumentName(int index) {
		ArgumentMapping argumentMapping = m_arguments.get(index);
		if (argumentMapping != null) {
			return argumentMapping.getName();
		}
		
		return null;
	}
	
	public void setArgumentName(int index, String name) {
		ArgumentMapping argumentMapping = m_arguments.get(index);
		if (argumentMapping == null) {
			argumentMapping = new ArgumentMapping(index, name);
			boolean wasAdded = m_arguments.put(index, argumentMapping) == null;
			assert (wasAdded);
		} else {
			argumentMapping.setName(name);
		}
	}
	
	public void removeArgumentName(int index) {
		boolean wasRemoved = m_arguments.remove(index) != null;
		assert (wasRemoved);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("\t");
		buf.append(m_obfName);
		buf.append(" <-> ");
		buf.append(m_deobfName);
		buf.append("\n");
		buf.append("\t");
		buf.append(m_obfSignature);
		buf.append("\n");
		buf.append("\tArguments:\n");
		for (ArgumentMapping argumentMapping : m_arguments.values()) {
			buf.append("\t\t");
			buf.append(argumentMapping.getIndex());
			buf.append(" -> ");
			buf.append(argumentMapping.getName());
			buf.append("\n");
		}
		return buf.toString();
	}
	
	@Override
	public int compareTo(MethodMapping other) {
		return (m_obfName + m_obfSignature).compareTo(other.m_obfName + other.m_obfSignature);
	}
	
	public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {
		
		// rename obf classes in the signature
		Signature newSignature = new Signature(m_obfSignature, new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				if (className.equals(oldObfClassName)) {
					return newObfClassName;
				}
				return null;
			}
		});
		
		if (!newSignature.equals(m_obfSignature)) {
			m_obfSignature = newSignature;
			return true;
		}
		return false;
	}
	
	public boolean containsArgument(String name) {
		for (ArgumentMapping argumentMapping : m_arguments.values()) {
			if (argumentMapping.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
}
