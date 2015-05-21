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
import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Maps;

public class ClassMapping implements Serializable, Comparable<ClassMapping> {
	
	private static final long serialVersionUID = -5148491146902340107L;
	
	private String m_obfFullName;
	private String m_obfSimpleName;
	private String m_deobfName;
	private Map<String,ClassMapping> m_innerClassesByObfSimple;
	private Map<String,ClassMapping> m_innerClassesByDeobf;
	private Map<String,FieldMapping> m_fieldsByObf;
	private Map<String,FieldMapping> m_fieldsByDeobf;
	private Map<String,MethodMapping> m_methodsByObf;
	private Map<String,MethodMapping> m_methodsByDeobf;
	
	public ClassMapping(String obfFullName) {
		this(obfFullName, null);
	}
	
	public ClassMapping(String obfFullName, String deobfName) {
		m_obfFullName = obfFullName;
		ClassEntry classEntry = new ClassEntry(obfFullName);
		m_obfSimpleName = classEntry.isInnerClass() ? classEntry.getInnermostClassName() : classEntry.getSimpleName();
		m_deobfName = NameValidator.validateClassName(deobfName, false);
		m_innerClassesByObfSimple = Maps.newHashMap();
		m_innerClassesByDeobf = Maps.newHashMap();
		m_fieldsByObf = Maps.newHashMap();
		m_fieldsByDeobf = Maps.newHashMap();
		m_methodsByObf = Maps.newHashMap();
		m_methodsByDeobf = Maps.newHashMap();
	}
	
	public String getObfFullName() {
		return m_obfFullName;
	}
	
	public String getObfSimpleName() {
		return m_obfSimpleName;
	}
	
	public String getDeobfName() {
		return m_deobfName;
	}
	
	public void setDeobfName(String val) {
		m_deobfName = NameValidator.validateClassName(val, false);
	}
	
	//// INNER CLASSES ////////
	
	public Iterable<ClassMapping> innerClasses() {
		assert (m_innerClassesByObfSimple.size() >= m_innerClassesByDeobf.size());
		return m_innerClassesByObfSimple.values();
	}
	
	public void addInnerClassMapping(ClassMapping classMapping) {
		boolean obfWasAdded = m_innerClassesByObfSimple.put(classMapping.getObfSimpleName(), classMapping) == null;
		assert (obfWasAdded);
		if (classMapping.getDeobfName() != null) {
			assert (isSimpleClassName(classMapping.getDeobfName()));
			boolean deobfWasAdded = m_innerClassesByDeobf.put(classMapping.getDeobfName(), classMapping) == null;
			assert (deobfWasAdded);
		}
	}
	
	public void removeInnerClassMapping(ClassMapping classMapping) {
		boolean obfWasRemoved = m_innerClassesByObfSimple.remove(classMapping.getObfSimpleName()) != null;
		assert (obfWasRemoved);
		if (classMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = m_innerClassesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (deobfWasRemoved);
		}
	}
	
	public ClassMapping getOrCreateInnerClass(ClassEntry obfInnerClass) {
		ClassMapping classMapping = m_innerClassesByObfSimple.get(obfInnerClass.getInnermostClassName());
		if (classMapping == null) {
			classMapping = new ClassMapping(obfInnerClass.getName());
			boolean wasAdded = m_innerClassesByObfSimple.put(classMapping.getObfSimpleName(), classMapping) == null;
			assert (wasAdded);
		}
		return classMapping;
	}
	
	public ClassMapping getInnerClassByObfSimple(String obfSimpleName) {
		assert (isSimpleClassName(obfSimpleName));
		return m_innerClassesByObfSimple.get(obfSimpleName);
	}
	
	public ClassMapping getInnerClassByDeobf(String deobfName) {
		assert (isSimpleClassName(deobfName));
		return m_innerClassesByDeobf.get(deobfName);
	}
	
	public ClassMapping getInnerClassByDeobfThenObfSimple(String name) {
		ClassMapping classMapping = getInnerClassByDeobf(name);
		if (classMapping == null) {
			classMapping = getInnerClassByObfSimple(name);
		}
		return classMapping;
	}
	
	public String getDeobfInnerClassName(String obfSimpleName) {
		assert (isSimpleClassName(obfSimpleName));
		ClassMapping classMapping = m_innerClassesByObfSimple.get(obfSimpleName);
		if (classMapping != null) {
			return classMapping.getDeobfName();
		}
		return null;
	}
	
	public void setInnerClassName(ClassEntry obfInnerClass, String deobfName) {
		ClassMapping classMapping = getOrCreateInnerClass(obfInnerClass);
		if (classMapping.getDeobfName() != null) {
			boolean wasRemoved = m_innerClassesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (wasRemoved);
		}
		classMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			assert (isSimpleClassName(deobfName));
			boolean wasAdded = m_innerClassesByDeobf.put(deobfName, classMapping) == null;
			assert (wasAdded);
		}
	}
	
	public boolean hasInnerClassByObfSimple(String obfSimpleName) {
		return m_innerClassesByObfSimple.containsKey(obfSimpleName);
	}
	
	public boolean hasInnerClassByDeobf(String deobfName) {
		return m_innerClassesByDeobf.containsKey(deobfName);
	}
	
	
	//// FIELDS ////////
	
	public Iterable<FieldMapping> fields() {
		assert (m_fieldsByObf.size() == m_fieldsByDeobf.size());
		return m_fieldsByObf.values();
	}
	
	public boolean containsObfField(String obfName, Type obfType) {
		return m_fieldsByObf.containsKey(getFieldKey(obfName, obfType));
	}
	
	public boolean containsDeobfField(String deobfName, Type deobfType) {
		return m_fieldsByDeobf.containsKey(getFieldKey(deobfName, deobfType));
	}
	
	public void addFieldMapping(FieldMapping fieldMapping) {
		String obfKey = getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType());
		if (m_fieldsByObf.containsKey(obfKey)) {
			throw new Error("Already have mapping for " + m_obfFullName + "." + obfKey);
		}
		String deobfKey = getFieldKey(fieldMapping.getDeobfName(), fieldMapping.getObfType());
		if (m_fieldsByDeobf.containsKey(deobfKey)) {
			throw new Error("Already have mapping for " + m_deobfName + "." + deobfKey);
		}
		boolean obfWasAdded = m_fieldsByObf.put(obfKey, fieldMapping) == null;
		assert (obfWasAdded);
		boolean deobfWasAdded = m_fieldsByDeobf.put(deobfKey, fieldMapping) == null;
		assert (deobfWasAdded);
		assert (m_fieldsByObf.size() == m_fieldsByDeobf.size());
	}
	
	public void removeFieldMapping(FieldMapping fieldMapping) {
		boolean obfWasRemoved = m_fieldsByObf.remove(getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType())) != null;
		assert (obfWasRemoved);
		if (fieldMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = m_fieldsByDeobf.remove(getFieldKey(fieldMapping.getDeobfName(), fieldMapping.getObfType())) != null;
			assert (deobfWasRemoved);
		}
	}
	
	public FieldMapping getFieldByObf(String obfName, Type obfType) {
		return m_fieldsByObf.get(getFieldKey(obfName, obfType));
	}
	
	public FieldMapping getFieldByDeobf(String deobfName, Type obfType) {
		return m_fieldsByDeobf.get(getFieldKey(deobfName, obfType));
	}
	
	public String getObfFieldName(String deobfName, Type obfType) {
		FieldMapping fieldMapping = m_fieldsByDeobf.get(getFieldKey(deobfName, obfType));
		if (fieldMapping != null) {
			return fieldMapping.getObfName();
		}
		return null;
	}
	
	public String getDeobfFieldName(String obfName, Type obfType) {
		FieldMapping fieldMapping = m_fieldsByObf.get(getFieldKey(obfName, obfType));
		if (fieldMapping != null) {
			return fieldMapping.getDeobfName();
		}
		return null;
	}
	
	private String getFieldKey(String name, Type type) {
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null!");
		}
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null!");
		}
		return name + ":" + type;
	}
	
	
	public void setFieldName(String obfName, Type obfType, String deobfName) {
		assert(deobfName != null);
		FieldMapping fieldMapping = m_fieldsByObf.get(getFieldKey(obfName, obfType));
		if (fieldMapping == null) {
			fieldMapping = new FieldMapping(obfName, obfType, deobfName);
			boolean obfWasAdded = m_fieldsByObf.put(getFieldKey(obfName, obfType), fieldMapping) == null;
			assert (obfWasAdded);
		} else {
			boolean wasRemoved = m_fieldsByDeobf.remove(getFieldKey(fieldMapping.getDeobfName(), obfType)) != null;
			assert (wasRemoved);
		}
		fieldMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			boolean wasAdded = m_fieldsByDeobf.put(getFieldKey(deobfName, obfType), fieldMapping) == null;
			assert (wasAdded);
		}
	}
	
	public void setFieldObfNameAndType(String oldObfName, Type obfType, String newObfName, Type newObfType) {
		assert(newObfName != null);
		FieldMapping fieldMapping = m_fieldsByObf.remove(getFieldKey(oldObfName, obfType));
		assert(fieldMapping != null);
		fieldMapping.setObfName(newObfName);
		fieldMapping.setObfType(newObfType);
		boolean obfWasAdded = m_fieldsByObf.put(getFieldKey(newObfName, newObfType), fieldMapping) == null;
		assert(obfWasAdded);
	}
	
	
	//// METHODS ////////
	
	public Iterable<MethodMapping> methods() {
		assert (m_methodsByObf.size() >= m_methodsByDeobf.size());
		return m_methodsByObf.values();
	}
	
	public boolean containsObfMethod(String obfName, Signature obfSignature) {
		return m_methodsByObf.containsKey(getMethodKey(obfName, obfSignature));
	}
	
	public boolean containsDeobfMethod(String deobfName, Signature obfSignature) {
		return m_methodsByDeobf.containsKey(getMethodKey(deobfName, obfSignature));
	}
	
	public void addMethodMapping(MethodMapping methodMapping) {
		String obfKey = getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature());
		if (m_methodsByObf.containsKey(obfKey)) {
			throw new Error("Already have mapping for " + m_obfFullName + "." + obfKey);
		}
		boolean wasAdded = m_methodsByObf.put(obfKey, methodMapping) == null;
		assert (wasAdded);
		if (methodMapping.getDeobfName() != null) {
			String deobfKey = getMethodKey(methodMapping.getDeobfName(), methodMapping.getObfSignature());
			if (m_methodsByDeobf.containsKey(deobfKey)) {
				throw new Error("Already have mapping for " + m_deobfName + "." + deobfKey);
			}
			boolean deobfWasAdded = m_methodsByDeobf.put(deobfKey, methodMapping) == null;
			assert (deobfWasAdded);
		}
		assert (m_methodsByObf.size() >= m_methodsByDeobf.size());
	}
	
	public void removeMethodMapping(MethodMapping methodMapping) {
		boolean obfWasRemoved = m_methodsByObf.remove(getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature())) != null;
		assert (obfWasRemoved);
		if (methodMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = m_methodsByDeobf.remove(getMethodKey(methodMapping.getDeobfName(), methodMapping.getObfSignature())) != null;
			assert (deobfWasRemoved);
		}
	}
	
	public MethodMapping getMethodByObf(String obfName, Signature obfSignature) {
		return m_methodsByObf.get(getMethodKey(obfName, obfSignature));
	}
	
	public MethodMapping getMethodByDeobf(String deobfName, Signature obfSignature) {
		return m_methodsByDeobf.get(getMethodKey(deobfName, obfSignature));
	}
	
	private String getMethodKey(String name, Signature signature) {
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null!");
		}
		if (signature == null) {
			throw new IllegalArgumentException("signature cannot be null!");
		}
		return name + signature;
	}
	
	public void setMethodName(String obfName, Signature obfSignature, String deobfName) {
		MethodMapping methodMapping = m_methodsByObf.get(getMethodKey(obfName, obfSignature));
		if (methodMapping == null) {
			methodMapping = createMethodMapping(obfName, obfSignature);
		} else if (methodMapping.getDeobfName() != null) {
			boolean wasRemoved = m_methodsByDeobf.remove(getMethodKey(methodMapping.getDeobfName(), methodMapping.getObfSignature())) != null;
			assert (wasRemoved);
		}
		methodMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			boolean wasAdded = m_methodsByDeobf.put(getMethodKey(deobfName, obfSignature), methodMapping) == null;
			assert (wasAdded);
		}
	}
	
	public void setMethodObfNameAndSignature(String oldObfName, Signature obfSignature, String newObfName, Signature newObfSignature) {
		assert(newObfName != null);
		MethodMapping methodMapping = m_methodsByObf.remove(getMethodKey(oldObfName, obfSignature));
		assert(methodMapping != null);
		methodMapping.setObfName(newObfName);
		methodMapping.setObfSignature(newObfSignature);
		boolean obfWasAdded = m_methodsByObf.put(getMethodKey(newObfName, newObfSignature), methodMapping) == null;
		assert(obfWasAdded);
	}
	
	//// ARGUMENTS ////////
	
	public void setArgumentName(String obfMethodName, Signature obfMethodSignature, int argumentIndex, String argumentName) {
		assert(argumentName != null);
		MethodMapping methodMapping = m_methodsByObf.get(getMethodKey(obfMethodName, obfMethodSignature));
		if (methodMapping == null) {
			methodMapping = createMethodMapping(obfMethodName, obfMethodSignature);
		}
		methodMapping.setArgumentName(argumentIndex, argumentName);
	}
	
	public void removeArgumentName(String obfMethodName, Signature obfMethodSignature, int argumentIndex) {
		m_methodsByObf.get(getMethodKey(obfMethodName, obfMethodSignature)).removeArgumentName(argumentIndex);
	}
	
	private MethodMapping createMethodMapping(String obfName, Signature obfSignature) {
		MethodMapping methodMapping = new MethodMapping(obfName, obfSignature);
		boolean wasAdded = m_methodsByObf.put(getMethodKey(obfName, obfSignature), methodMapping) == null;
		assert (wasAdded);
		return methodMapping;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(m_obfFullName);
		buf.append(" <-> ");
		buf.append(m_deobfName);
		buf.append("\n");
		buf.append("Fields:\n");
		for (FieldMapping fieldMapping : fields()) {
			buf.append("\t");
			buf.append(fieldMapping.getObfName());
			buf.append(" <-> ");
			buf.append(fieldMapping.getDeobfName());
			buf.append("\n");
		}
		buf.append("Methods:\n");
		for (MethodMapping methodMapping : m_methodsByObf.values()) {
			buf.append(methodMapping.toString());
			buf.append("\n");
		}
		buf.append("Inner Classes:\n");
		for (ClassMapping classMapping : m_innerClassesByObfSimple.values()) {
			buf.append("\t");
			buf.append(classMapping.getObfSimpleName());
			buf.append(" <-> ");
			buf.append(classMapping.getDeobfName());
			buf.append("\n");
		}
		return buf.toString();
	}
	
	@Override
	public int compareTo(ClassMapping other) {
		// sort by a, b, c, ... aa, ab, etc
		if (m_obfFullName.length() != other.m_obfFullName.length()) {
			return m_obfFullName.length() - other.m_obfFullName.length();
		}
		return m_obfFullName.compareTo(other.m_obfFullName);
	}
	
	public boolean renameObfClass(String oldObfClassName, String newObfClassName) {
		
		// rename inner classes
		for (ClassMapping innerClassMapping : new ArrayList<ClassMapping>(m_innerClassesByObfSimple.values())) {
			if (innerClassMapping.renameObfClass(oldObfClassName, newObfClassName)) {
				boolean wasRemoved = m_innerClassesByObfSimple.remove(oldObfClassName) != null;
				assert (wasRemoved);
				boolean wasAdded = m_innerClassesByObfSimple.put(newObfClassName, innerClassMapping) == null;
				assert (wasAdded);
			}
		}
		
		// rename field types
		for (FieldMapping fieldMapping : new ArrayList<FieldMapping>(m_fieldsByObf.values())) {
			String oldFieldKey = getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType());
			if (fieldMapping.renameObfClass(oldObfClassName, newObfClassName)) {
				boolean wasRemoved = m_fieldsByObf.remove(oldFieldKey) != null;
				assert (wasRemoved);
				boolean wasAdded = m_fieldsByObf.put(getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType()), fieldMapping) == null;
				assert (wasAdded);
			}
		}
		
		// rename method signatures
		for (MethodMapping methodMapping : new ArrayList<MethodMapping>(m_methodsByObf.values())) {
			String oldMethodKey = getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature());
			if (methodMapping.renameObfClass(oldObfClassName, newObfClassName)) {
				boolean wasRemoved = m_methodsByObf.remove(oldMethodKey) != null;
				assert (wasRemoved);
				boolean wasAdded = m_methodsByObf.put(getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature()), methodMapping) == null;
				assert (wasAdded);
			}
		}
		
		if (m_obfFullName.equals(oldObfClassName)) {
			// rename this class
			m_obfFullName = newObfClassName;
			return true;
		}
		return false;
	}
	
	public boolean containsArgument(BehaviorEntry obfBehaviorEntry, String name) {
		MethodMapping methodMapping = m_methodsByObf.get(getMethodKey(obfBehaviorEntry.getName(), obfBehaviorEntry.getSignature()));
		if (methodMapping != null) {
			return methodMapping.containsArgument(name);
		}
		return false;
	}
	
	public static boolean isSimpleClassName(String name) {
		return name.indexOf('/') < 0 && name.indexOf('$') < 0;
	}

	public ClassEntry getObfEntry() {
		return new ClassEntry(m_obfFullName);
	}
}
