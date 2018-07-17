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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cuchaz.enigma.analysis.TranslationIndex;
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.entry.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectionalTranslator implements Translator {
	private final TranslationDirection direction;
	private final Map<String, ClassMapping> classes;
	private final TranslationIndex index;

	public DirectionalTranslator(ReferencedEntryPool entryPool) {
		this.direction = null;
		this.classes = Maps.newHashMap();
		this.index = new TranslationIndex(entryPool);
	}

	public DirectionalTranslator(TranslationDirection direction, Map<String, ClassMapping> classes, TranslationIndex index) {
		this.direction = direction;
		this.classes = classes;
		this.index = index;
	}

	public TranslationDirection getDirection() {
		return direction;
	}

	public TranslationIndex getTranslationIndex() {
		return index;
	}

	@Override
	public ClassEntry getTranslatedClass(ClassEntry entry) {
		String className;
		if (entry.isArray()) {
			className = this.getTranslatedTypeDesc(new TypeDescriptor(entry.getName())).toString();
		} else {
			className = entry.isInnerClass() ? translateInnerClassName(entry) : translateClassName(entry);
		}
		return new ClassEntry(className);
	}

	@Override
	public ClassDefEntry getTranslatedClassDef(ClassDefEntry entry) {
		String className;
		if (entry.isArray()) {
			className = this.getTranslatedTypeDesc(new TypeDescriptor(entry.getName())).toString();
		} else {
			className = entry.isInnerClass() ? translateInnerClassName(entry) : translateClassName(entry);
		}
		Signature translatedSignature = this.getTranslatedSignature(entry.getSignature());
		return new ClassDefEntry(className, translatedSignature, getClassModifier(entry).transform(entry.getAccess()));
	}

	private String translateClassName(ClassEntry entry) {
		// normal classes are easy
		ClassMapping classMapping = this.classes.get(entry.getName());
		if (classMapping == null) {
			return entry.getName();
		}
		return classMapping.getTranslatedName(direction);
	}

	private String translateInnerClassName(ClassEntry entry) {
		// translate as much of the class chain as we can
		List<ClassMapping> mappingsChain = getClassMappingChain(entry);
		String[] obfClassNames = entry.getName().split("\\$");
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < obfClassNames.length; i++) {
			boolean isFirstClass = buf.length() == 0;
			String className = null;
			ClassMapping classMapping = mappingsChain.get(i);
			if (classMapping != null) {
				className = this.direction.choose(
						classMapping.getDeobfName(),
						isFirstClass ? classMapping.getObfFullName() : classMapping.getObfSimpleName()
				);
			}
			if (className == null) {
				className = obfClassNames[i];
			}
			if (!isFirstClass) {
				buf.append("$");
			}
			buf.append(className);
		}
		return buf.toString();
	}

	@Override
	public FieldDefEntry getTranslatedFieldDef(FieldDefEntry entry) {
		String translatedName = translateFieldName(entry);
		if (translatedName == null) {
			translatedName = entry.getName();
		}
		ClassEntry translatedOwner = getTranslatedClass(entry.getOwnerClassEntry());
		TypeDescriptor translatedDesc = getTranslatedTypeDesc(entry.getDesc());
		Signature translatedSignature = getTranslatedSignature(entry.getSignature());
		AccessFlags translatedAccess = getFieldModifier(entry).transform(entry.getAccess());
		return new FieldDefEntry(translatedOwner, translatedName, translatedDesc, translatedSignature, translatedAccess);
	}

	@Override
	public FieldEntry getTranslatedField(FieldEntry entry) {
		String translatedName = translateFieldName(entry);
		if (translatedName == null) {
			translatedName = entry.getName();
		}
		ClassEntry translatedOwner = getTranslatedClass(entry.getOwnerClassEntry());
		TypeDescriptor translatedDesc = getTranslatedTypeDesc(entry.getDesc());
		return new FieldEntry(translatedOwner, translatedName, translatedDesc);
	}

	private String translateFieldName(FieldEntry entry) {
		// resolve the class entry
		ClassEntry resolvedClassEntry = this.index.resolveEntryOwner(entry, true);
		if (resolvedClassEntry != null) {
			// look for the class
			ClassMapping classMapping = findClassMapping(resolvedClassEntry);
			if (classMapping != null) {
				// look for the field
				FieldMapping mapping = this.direction.choose(
						classMapping.getFieldByObf(entry.getName(), entry.getDesc()),
						classMapping.getFieldByDeobf(entry.getName(), getTranslatedTypeDesc(entry.getDesc()))
				);
				if (mapping != null) {
					return this.direction.choose(mapping.getDeobfName(), mapping.getObfName());
				}
			}
		}
		return null;
	}

	@Override
	public MethodDefEntry getTranslatedMethodDef(MethodDefEntry entry) {
		String translatedName = translateMethodName(entry);
		if (translatedName == null) {
			translatedName = entry.getName();
		}
		ClassEntry translatedOwner = getTranslatedClass(entry.getOwnerClassEntry());
		MethodDescriptor translatedDesc = getTranslatedMethodDesc(entry.getDesc());
		Signature translatedSignature = getTranslatedSignature(entry.getSignature());
		AccessFlags access = getMethodModifier(entry).transform(entry.getAccess());
		return new MethodDefEntry(translatedOwner, translatedName, translatedDesc, translatedSignature, access);
	}

	@Override
	public MethodEntry getTranslatedMethod(MethodEntry entry) {
		String translatedName = translateMethodName(entry);
		if (translatedName == null) {
			translatedName = entry.getName();
		}
		ClassEntry translatedOwner = getTranslatedClass(entry.getOwnerClassEntry());
		MethodDescriptor translatedDesc = getTranslatedMethodDesc(entry.getDesc());
		return new MethodEntry(translatedOwner, translatedName, translatedDesc);
	}

	private String translateMethodName(MethodEntry entry) {
		// resolve the class entry
		ClassEntry resolvedOwner = this.index.resolveEntryOwner(entry, true);
		if (resolvedOwner != null) {
			// look for class
			ClassMapping classMapping = findClassMapping(resolvedOwner);
			if (classMapping != null) {
				// look for the method
				MethodMapping mapping = this.direction.choose(
						classMapping.getMethodByObf(entry.getName(), entry.getDesc()),
						classMapping.getMethodByDeobf(entry.getName(), getTranslatedMethodDesc(entry.getDesc()))
				);
				if (mapping != null) {
					return this.direction.choose(mapping.getDeobfName(), mapping.getObfName());
				}
			}
		}
		return null;
	}

	@Override
	public LocalVariableEntry getTranslatedVariable(LocalVariableEntry entry) {
		String translatedArgumentName = translateLocalVariableName(entry);
		if (translatedArgumentName == null) {
			translatedArgumentName = inheritLocalVariableName(entry);
		}
		if (translatedArgumentName == null) {
			translatedArgumentName = entry.getName();
		}
		// TODO: Translating arguments calls method translation.. Can we refactor the code in such a way that we don't need this?
		MethodEntry translatedOwner = getTranslatedMethod(entry.getOwnerEntry());
		return new LocalVariableEntry(translatedOwner, entry.getIndex(), translatedArgumentName);
	}

	@Override
	public LocalVariableDefEntry getTranslatedVariableDef(LocalVariableDefEntry entry) {
		String translatedArgumentName = translateLocalVariableName(entry);
		if (translatedArgumentName == null) {
			translatedArgumentName = inheritLocalVariableName(entry);
		}
		// TODO: Translating arguments calls method translation.. Can we refactor the code in such a way that we don't need this?
		MethodDefEntry translatedOwner = getTranslatedMethodDef(entry.getOwnerEntry());
		TypeDescriptor translatedTypeDesc = getTranslatedTypeDesc(entry.getDesc());
		return new LocalVariableDefEntry(translatedOwner, entry.getIndex(), translatedArgumentName != null ? translatedArgumentName : entry.getName(), translatedTypeDesc);
	}

	@Override
	public boolean hasClassMapping(ClassEntry entry) {
		return classes.containsKey(entry.getName());
	}

	@Override
	public boolean hasFieldMapping(FieldEntry entry) {
		return translateFieldName(entry) != null;
	}

	@Override
	public boolean hasMethodMapping(MethodEntry entry) {
		return translateMethodName(entry) != null;
	}

	@Override
	public boolean hasLocalVariableMapping(LocalVariableEntry entry) {
		return translateLocalVariableName(entry) != null || inheritLocalVariableName(entry) != null;
	}

	// TODO: support not identical behavior (specific to constructor)
	private String translateLocalVariableName(LocalVariableEntry entry) {
		// look for identical behavior in superclasses
		ClassEntry ownerEntry = entry.getOwnerClassEntry();
		if (ownerEntry != null) {
			// look for the class
			ClassMapping classMapping = findClassMapping(ownerEntry);
			if (classMapping != null) {
				// look for the method
				MethodMapping methodMapping = this.direction.choose(
						classMapping.getMethodByObf(entry.getMethodName(), entry.getMethodDesc()),
						classMapping.getMethodByDeobf(entry.getMethodName(), getTranslatedMethodDesc(entry.getMethodDesc()))
				);
				if (methodMapping != null) {
					int index = entry.getIndex();
					return this.direction.choose(
							methodMapping.getDeobfLocalVariableName(index),
							methodMapping.getObfLocalVariableName(index)
					);
				}
			}
		}
		return null;
	}

	private String inheritLocalVariableName(LocalVariableEntry entry) {
		List<ClassEntry> ancestry = this.index.getAncestry(entry.getOwnerClassEntry());
		// Check in mother class for the arg
		for (ClassEntry ancestorEntry : ancestry) {
			LocalVariableEntry motherArg = entry.updateOwnership(ancestorEntry);
			if (this.index.entryExists(motherArg)) {
				String result = translateLocalVariableName(motherArg);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	@Override
	public TypeDescriptor getTranslatedTypeDesc(TypeDescriptor desc) {
		return desc.remap(this::remapClass);
	}

	@Override
	public MethodDescriptor getTranslatedMethodDesc(MethodDescriptor descriptor) {
		List<TypeDescriptor> arguments = descriptor.getArgumentDescs();
		List<TypeDescriptor> translatedArguments = new ArrayList<>(arguments.size());
		for (TypeDescriptor argument : arguments) {
			translatedArguments.add(getTranslatedTypeDesc(argument));
		}
		return new MethodDescriptor(translatedArguments, getTranslatedTypeDesc(descriptor.getReturnDesc()));
	}

	@Override
	public Signature getTranslatedSignature(Signature signature) {
		if (signature == null) {
			return null;
		}
		return signature.remap(this::remapClass);
	}

	private ClassMapping findClassMapping(ClassEntry entry) {
		List<ClassMapping> mappingChain = getClassMappingChain(entry);
		return mappingChain.get(mappingChain.size() - 1);
	}

	private List<ClassMapping> getClassMappingChain(ClassEntry entry) {

		// get a list of all the classes in the hierarchy
		String[] parts = entry.getName().split("\\$");
		List<ClassMapping> mappingsChain = Lists.newArrayList();

		// get mappings for the outer class
		ClassMapping outerClassMapping = this.classes.get(parts[0]);
		mappingsChain.add(outerClassMapping);

		for (int i = 1; i < parts.length; i++) {

			// get mappings for the inner class
			ClassMapping innerClassMapping = null;
			if (outerClassMapping != null) {
				innerClassMapping = this.direction.choose(
						outerClassMapping.getInnerClassByObfSimple(parts[i]),
						outerClassMapping.getInnerClassByDeobfThenObfSimple(parts[i])
				);
			}
			mappingsChain.add(innerClassMapping);
			outerClassMapping = innerClassMapping;
		}

		assert (mappingsChain.size() == parts.length);
		return mappingsChain;
	}

	private Mappings.EntryModifier getClassModifier(ClassEntry entry) {
		ClassMapping classMapping = findClassMapping(entry);
		if (classMapping != null) {
			return classMapping.getModifier();
		}
		return Mappings.EntryModifier.UNCHANGED;
	}

	private Mappings.EntryModifier getFieldModifier(FieldEntry entry) {
		ClassMapping classMapping = findClassMapping(entry.getOwnerClassEntry());
		if (classMapping != null) {
			FieldMapping fieldMapping = classMapping.getFieldByObf(entry);
			if (fieldMapping != null) {
				return fieldMapping.getModifier();
			}
		}
		return Mappings.EntryModifier.UNCHANGED;
	}

	private Mappings.EntryModifier getMethodModifier(MethodEntry entry) {
		ClassMapping classMapping = findClassMapping(entry.getOwnerClassEntry());
		if (classMapping != null) {
			MethodMapping methodMapping = classMapping.getMethodByObf(entry);
			if (methodMapping != null) {
				return methodMapping.getModifier();
			}
		}
		return Mappings.EntryModifier.UNCHANGED;
	}

	private String remapClass(String name) {
		return getTranslatedClass(new ClassEntry(name)).getName();
	}
}
