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

import com.google.common.collect.Maps;
import cuchaz.enigma.throwables.MappingConflict;

import java.util.ArrayList;
import java.util.Map;

// FIXME: Enigma doesn't support inner classes of inner class????!
public class ClassMapping implements Comparable<ClassMapping> {

	private String obfFullName;
	private String obfSimpleName;
	private String deobfName;
	private String previousDeobfName;
	private Map<String, ClassMapping> innerClassesByObfSimple;
	private Map<String, ClassMapping> innerClassesByObfFull;
	private Map<String, ClassMapping> innerClassesByDeobf;
	private Map<String, FieldMapping> fieldsByObf;
	private Map<String, FieldMapping> fieldsByDeobf;
	private Map<String, MethodMapping> methodsByObf;
	private Map<String, MethodMapping> methodsByDeobf;
	private boolean isDirty;
	private Mappings.EntryModifier modifier;

	public ClassMapping(String obfFullName) {
		this(obfFullName, null, Mappings.EntryModifier.UNCHANGED);
	}

	public ClassMapping(String obfFullName, String deobfName) {
		this(obfFullName, deobfName, Mappings.EntryModifier.UNCHANGED);
	}

	public ClassMapping(String obfFullName, String deobfName, Mappings.EntryModifier modifier) {
		this.obfFullName = obfFullName;
		ClassEntry classEntry = new ClassEntry(obfFullName);
		obfSimpleName = classEntry.isInnerClass() ? classEntry.getInnermostClassName() : classEntry.getSimpleName();
		previousDeobfName = null;
		this.deobfName = NameValidator.validateClassName(deobfName, false);
		innerClassesByObfSimple = Maps.newHashMap();
		innerClassesByObfFull = Maps.newHashMap();
		innerClassesByDeobf = Maps.newHashMap();
		fieldsByObf = Maps.newHashMap();
		fieldsByDeobf = Maps.newHashMap();
		methodsByObf = Maps.newHashMap();
		methodsByDeobf = Maps.newHashMap();
		isDirty = true;
		this.modifier = modifier;
	}

	public static boolean isSimpleClassName(String name) {
		return name.indexOf('/') < 0 && name.indexOf('$') < 0;
	}

	public String getObfFullName() {
		return obfFullName;
	}

	public String getObfSimpleName() {
		return obfSimpleName;
	}

	public String getPreviousDeobfName() {
		return previousDeobfName;
	}

	public String getDeobfName() {
		return deobfName;
	}

	//// INNER CLASSES ////////

	public void setDeobfName(String val) {
		previousDeobfName = deobfName;
		deobfName = NameValidator.validateClassName(val, false);
		this.isDirty = true;
	}

	public Iterable<ClassMapping> innerClasses() {
		assert (innerClassesByObfSimple.size() >= innerClassesByDeobf.size());
		return innerClassesByObfSimple.values();
	}

	public void addInnerClassMapping(ClassMapping classMapping) throws MappingConflict {
		// FIXME: dirty hack, that can get into issues, but it's a temp fix!
		if (this.innerClassesByObfFull.containsKey(classMapping.getObfSimpleName())) {
			throw new MappingConflict("classes", classMapping.getObfSimpleName(), this.innerClassesByObfSimple.get(classMapping.getObfSimpleName()).getObfSimpleName());
		}
		innerClassesByObfFull.put(classMapping.getObfFullName(), classMapping);
		innerClassesByObfSimple.put(classMapping.getObfSimpleName(), classMapping);

		if (classMapping.getDeobfName() != null) {
			if (this.innerClassesByDeobf.containsKey(classMapping.getDeobfName())) {
				throw new MappingConflict("classes", classMapping.getDeobfName(), this.innerClassesByDeobf.get(classMapping.getDeobfName()).getDeobfName());
			}
			innerClassesByDeobf.put(classMapping.getDeobfName(), classMapping);
		}
		this.isDirty = true;
	}

	public void removeInnerClassMapping(ClassMapping classMapping) {
		innerClassesByObfFull.remove(classMapping.getObfFullName());
		boolean obfWasRemoved = innerClassesByObfSimple.remove(classMapping.getObfSimpleName()) != null;
		assert (obfWasRemoved);
		if (classMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = innerClassesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (deobfWasRemoved);
		}
		this.isDirty = true;
	}

	public ClassMapping getOrCreateInnerClass(ClassEntry obfInnerClass) {
		ClassMapping classMapping = innerClassesByObfSimple.get(obfInnerClass.getInnermostClassName());
		if (classMapping == null) {
			classMapping = new ClassMapping(obfInnerClass.getName());
			innerClassesByObfFull.put(classMapping.getObfFullName(), classMapping);
			boolean wasAdded = innerClassesByObfSimple.put(classMapping.getObfSimpleName(), classMapping) == null;
			assert (wasAdded);
			this.isDirty = true;
		}
		return classMapping;
	}

	public ClassMapping getInnerClassByObfSimple(String obfSimpleName) {
		assert (isSimpleClassName(obfSimpleName));
		return innerClassesByObfSimple.get(obfSimpleName);
	}

	public ClassMapping getInnerClassByDeobf(String deobfName) {
		assert (isSimpleClassName(deobfName));
		return innerClassesByDeobf.get(deobfName);
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
		ClassMapping classMapping = innerClassesByObfSimple.get(obfSimpleName);
		if (classMapping != null) {
			return classMapping.getDeobfName();
		}
		return null;
	}

	public void setInnerClassName(ClassEntry obfInnerClass, String deobfName) {
		ClassMapping classMapping = getOrCreateInnerClass(obfInnerClass);
		if (classMapping.getDeobfName() != null) {
			boolean wasRemoved = innerClassesByDeobf.remove(classMapping.getDeobfName()) != null;
			assert (wasRemoved);
		}
		classMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			assert (isSimpleClassName(deobfName));
			boolean wasAdded = innerClassesByDeobf.put(deobfName, classMapping) == null;
			assert (wasAdded);
		}
		this.isDirty = true;
	}

	public boolean hasInnerClassByObfSimple(String obfSimpleName) {
		return innerClassesByObfSimple.containsKey(obfSimpleName);
	}

	//// FIELDS ////////

	public boolean hasInnerClassByDeobf(String deobfName) {
		return innerClassesByDeobf.containsKey(deobfName);
	}

	public Iterable<FieldMapping> fields() {
		assert (fieldsByObf.size() == fieldsByDeobf.size());
		return fieldsByObf.values();
	}

	public boolean containsObfField(String obfName, Type obfType) {
		return fieldsByObf.containsKey(getFieldKey(obfName, obfType));
	}

	public boolean containsDeobfField(String deobfName, Type deobfType) {
		return fieldsByDeobf.containsKey(getFieldKey(deobfName, deobfType));
	}

	public void addFieldMapping(FieldMapping fieldMapping) {
		String obfKey = getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType());
		if (fieldsByObf.containsKey(obfKey)) {
			throw new Error("Already have mapping for " + obfFullName + "." + obfKey);
		}
		if (fieldMapping.getDeobfName() != null) {
			String deobfKey = getFieldKey(fieldMapping.getDeobfName(), fieldMapping.getObfType());
			if (fieldsByDeobf.containsKey(deobfKey)) {
				throw new Error("Already have mapping for " + deobfName + "." + deobfKey);
			}
			boolean deobfWasAdded = fieldsByDeobf.put(deobfKey, fieldMapping) == null;
			assert (deobfWasAdded);
		}
		boolean obfWasAdded = fieldsByObf.put(obfKey, fieldMapping) == null;
		assert (obfWasAdded);
		this.isDirty = true;
	}

	public void removeFieldMapping(FieldMapping fieldMapping) {
		boolean obfWasRemoved = fieldsByObf.remove(getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType())) != null;
		assert (obfWasRemoved);
		if (fieldMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = fieldsByDeobf.remove(getFieldKey(fieldMapping.getDeobfName(), fieldMapping.getObfType())) != null;
			assert (deobfWasRemoved);
		}
		this.isDirty = true;
	}

	public FieldMapping getFieldByObf(String obfName, Type obfType) {
		return fieldsByObf.get(getFieldKey(obfName, obfType));
	}

	public FieldMapping getFieldByDeobf(String deobfName, Type obfType) {
		return fieldsByDeobf.get(getFieldKey(deobfName, obfType));
	}

	public String getObfFieldName(String deobfName, Type obfType) {
		FieldMapping fieldMapping = fieldsByDeobf.get(getFieldKey(deobfName, obfType));
		if (fieldMapping != null) {
			return fieldMapping.getObfName();
		}
		return null;
	}

	public String getDeobfFieldName(String obfName, Type obfType) {
		FieldMapping fieldMapping = fieldsByObf.get(getFieldKey(obfName, obfType));
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
		assert (deobfName != null);
		FieldMapping fieldMapping = fieldsByObf.get(getFieldKey(obfName, obfType));
		if (fieldMapping == null) {
			fieldMapping = new FieldMapping(obfName, obfType, deobfName, Mappings.EntryModifier.UNCHANGED);
			boolean obfWasAdded = fieldsByObf.put(getFieldKey(obfName, obfType), fieldMapping) == null;
			assert (obfWasAdded);
		} else {
			boolean wasRemoved = fieldsByDeobf.remove(getFieldKey(fieldMapping.getDeobfName(), obfType)) != null;
			assert (wasRemoved);
		}
		fieldMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			boolean wasAdded = fieldsByDeobf.put(getFieldKey(deobfName, obfType), fieldMapping) == null;
			assert (wasAdded);
		}
		this.isDirty = true;
	}

	//// METHODS ////////

	public void setFieldObfNameAndType(String oldObfName, Type obfType, String newObfName, Type newObfType) {
		assert (newObfName != null);
		FieldMapping fieldMapping = fieldsByObf.remove(getFieldKey(oldObfName, obfType));
		assert (fieldMapping != null);
		fieldMapping.setObfName(newObfName);
		fieldMapping.setObfType(newObfType);
		boolean obfWasAdded = fieldsByObf.put(getFieldKey(newObfName, newObfType), fieldMapping) == null;
		assert (obfWasAdded);
		this.isDirty = true;
	}

	public Iterable<MethodMapping> methods() {
		assert (methodsByObf.size() >= methodsByDeobf.size());
		return methodsByObf.values();
	}

	public boolean containsObfMethod(String obfName, Signature obfSignature) {
		return methodsByObf.containsKey(getMethodKey(obfName, obfSignature));
	}

	public boolean containsDeobfMethod(String deobfName, Signature obfSignature) {
		return methodsByDeobf.containsKey(getMethodKey(deobfName, obfSignature));
	}

	public void addMethodMapping(MethodMapping methodMapping) {
		String obfKey = getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature());
		if (methodsByObf.containsKey(obfKey)) {
			throw new Error("Already have mapping for " + obfFullName + "." + obfKey);
		}
		boolean wasAdded = methodsByObf.put(obfKey, methodMapping) == null;
		assert (wasAdded);
		if (methodMapping.getDeobfName() != null) {
			String deobfKey = getMethodKey(methodMapping.getDeobfName(), methodMapping.getObfSignature());
			if (methodsByDeobf.containsKey(deobfKey)) {
				throw new Error("Already have mapping for " + deobfName + "." + deobfKey);
			}
			boolean deobfWasAdded = methodsByDeobf.put(deobfKey, methodMapping) == null;
			assert (deobfWasAdded);
		}
		this.isDirty = true;
		assert (methodsByObf.size() >= methodsByDeobf.size());
	}

	public void removeMethodMapping(MethodMapping methodMapping) {
		boolean obfWasRemoved = methodsByObf.remove(getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature())) != null;
		assert (obfWasRemoved);
		if (methodMapping.getDeobfName() != null) {
			boolean deobfWasRemoved = methodsByDeobf.remove(getMethodKey(methodMapping.getDeobfName(), methodMapping.getObfSignature())) != null;
			assert (deobfWasRemoved);
		}
		this.isDirty = true;
	}

	public MethodMapping getMethodByObf(String obfName, Signature obfSignature) {
		return methodsByObf.get(getMethodKey(obfName, obfSignature));
	}

	public MethodMapping getMethodByDeobf(String deobfName, Signature obfSignature) {
		return methodsByDeobf.get(getMethodKey(deobfName, obfSignature));
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
		MethodMapping methodMapping = methodsByObf.get(getMethodKey(obfName, obfSignature));
		if (methodMapping == null) {
			methodMapping = createMethodMapping(obfName, obfSignature);
		} else if (methodMapping.getDeobfName() != null) {
			boolean wasRemoved = methodsByDeobf.remove(getMethodKey(methodMapping.getDeobfName(), methodMapping.getObfSignature())) != null;
			assert (wasRemoved);
		}
		methodMapping.setDeobfName(deobfName);
		if (deobfName != null) {
			boolean wasAdded = methodsByDeobf.put(getMethodKey(deobfName, obfSignature), methodMapping) == null;
			assert (wasAdded);
		}
		this.isDirty = true;
	}

	//// ARGUMENTS ////////

	public void setMethodObfNameAndSignature(String oldObfName, Signature obfSignature, String newObfName, Signature newObfSignature) {
		assert (newObfName != null);
		MethodMapping methodMapping = methodsByObf.remove(getMethodKey(oldObfName, obfSignature));
		assert (methodMapping != null);
		methodMapping.setObfName(newObfName);
		methodMapping.setObfSignature(newObfSignature);
		boolean obfWasAdded = methodsByObf.put(getMethodKey(newObfName, newObfSignature), methodMapping) == null;
		assert (obfWasAdded);
		this.isDirty = true;
	}

	public void setArgumentName(String obfMethodName, Signature obfMethodSignature, int argumentIndex, String argumentName) {
		assert (argumentName != null);
		MethodMapping methodMapping = methodsByObf.get(getMethodKey(obfMethodName, obfMethodSignature));
		if (methodMapping == null) {
			methodMapping = createMethodMapping(obfMethodName, obfMethodSignature);
		}
		methodMapping.setArgumentName(argumentIndex, argumentName);
		this.isDirty = true;
	}

	public void removeArgumentName(String obfMethodName, Signature obfMethodSignature, int argumentIndex) {
		methodsByObf.get(getMethodKey(obfMethodName, obfMethodSignature)).removeArgumentName(argumentIndex);
		this.isDirty = true;
	}

	private MethodMapping createMethodMapping(String obfName, Signature obfSignature) {
		MethodMapping methodMapping = new MethodMapping(obfName, obfSignature);
		boolean wasAdded = methodsByObf.put(getMethodKey(obfName, obfSignature), methodMapping) == null;
		assert (wasAdded);
		this.isDirty = true;
		return methodMapping;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(obfFullName);
		buf.append(" <-> ");
		buf.append(deobfName);
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
		for (MethodMapping methodMapping : methodsByObf.values()) {
			buf.append(methodMapping);
			buf.append("\n");
		}
		buf.append("Inner Classes:\n");
		for (ClassMapping classMapping : innerClassesByObfSimple.values()) {
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
		if (obfFullName.length() != other.obfFullName.length()) {
			return obfFullName.length() - other.obfFullName.length();
		}
		return obfFullName.compareTo(other.obfFullName);
	}

	public boolean renameObfClass(String oldObfClassName, String newObfClassName) {

		// rename inner classes
		for (ClassMapping innerClassMapping : new ArrayList<>(innerClassesByObfSimple.values())) {
			if (innerClassMapping.renameObfClass(oldObfClassName, newObfClassName)) {
				boolean wasRemoved = innerClassesByObfSimple.remove(oldObfClassName) != null;
				assert (wasRemoved);
				boolean wasAdded = innerClassesByObfSimple.put(newObfClassName, innerClassMapping) == null;
				assert (wasAdded);
			}
		}

		// rename field types
		for (FieldMapping fieldMapping : new ArrayList<>(fieldsByObf.values())) {
			String oldFieldKey = getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType());
			if (fieldMapping.renameObfClass(oldObfClassName, newObfClassName)) {
				boolean wasRemoved = fieldsByObf.remove(oldFieldKey) != null;
				assert (wasRemoved);
				boolean wasAdded = fieldsByObf
					.put(getFieldKey(fieldMapping.getObfName(), fieldMapping.getObfType()), fieldMapping) == null;
				assert (wasAdded);
			}
		}

		// rename method signatures
		for (MethodMapping methodMapping : new ArrayList<>(methodsByObf.values())) {
			String oldMethodKey = getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature());
			if (methodMapping.renameObfClass(oldObfClassName, newObfClassName)) {
				boolean wasRemoved = methodsByObf.remove(oldMethodKey) != null;
				assert (wasRemoved);
				boolean wasAdded = methodsByObf
					.put(getMethodKey(methodMapping.getObfName(), methodMapping.getObfSignature()), methodMapping) == null;
				assert (wasAdded);
			}
		}

		if (obfFullName.equals(oldObfClassName)) {
			// rename this class
			obfFullName = newObfClassName;
			return true;
		}
		this.isDirty = true;
		return false;
	}

	public boolean containsArgument(BehaviorEntry obfBehaviorEntry, String name) {
		MethodMapping methodMapping = methodsByObf.get(getMethodKey(obfBehaviorEntry.getName(), obfBehaviorEntry.getSignature()));
		return methodMapping != null && methodMapping.containsArgument(name);
	}

	public ClassEntry getObfEntry() {
		return new ClassEntry(obfFullName);
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void resetDirty() {
		this.isDirty = false;
	}

	public Mappings.EntryModifier getModifier() {
		return modifier;
	}

	public void setModifier(Mappings.EntryModifier modifier) {
		if (this.modifier != modifier)
			this.isDirty = true;
		this.modifier = modifier;
	}

	public void setFieldModifier(String obfName, Type obfType, Mappings.EntryModifier modifier) {
		FieldMapping fieldMapping = fieldsByObf.computeIfAbsent(getFieldKey(obfName, obfType),
			k -> new FieldMapping(obfName, obfType, null, Mappings.EntryModifier.UNCHANGED));

		if (fieldMapping.getModifier() != modifier) {
			fieldMapping.setModifier(modifier);
			this.isDirty = true;
		}
	}

	public void setMethodModifier(String obfName, Signature sig, Mappings.EntryModifier modifier) {
		MethodMapping methodMapping = methodsByObf.computeIfAbsent(getMethodKey(obfName, sig),
			k -> new MethodMapping(obfName, sig, null, Mappings.EntryModifier.UNCHANGED));

		if (methodMapping.getModifier() != modifier) {
			methodMapping.setModifier(modifier);
			this.isDirty = true;
		}
	}
}
