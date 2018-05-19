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

import cuchaz.enigma.mapping.entry.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

public interface Translator {
	ClassEntry getTranslatedClass(ClassEntry entry);

	ClassDefEntry getTranslatedClassDef(ClassDefEntry entry);

	FieldEntry getTranslatedField(FieldEntry entry);

	FieldDefEntry getTranslatedFieldDef(FieldDefEntry entry);

	MethodEntry getTranslatedMethod(MethodEntry entry);

	MethodDefEntry getTranslatedMethodDef(MethodDefEntry entry);

	LocalVariableEntry getTranslatedVariable(LocalVariableEntry entry);

	LocalVariableDefEntry getTranslatedVariableDef(LocalVariableDefEntry entry);

	boolean hasClassMapping(ClassEntry entry);

	boolean hasFieldMapping(FieldEntry entry);

	boolean hasMethodMapping(MethodEntry entry);

	boolean hasLocalVariableMapping(LocalVariableEntry entry);

	TypeDescriptor getTranslatedTypeDesc(TypeDescriptor desc);

	MethodDescriptor getTranslatedMethodDesc(MethodDescriptor descriptor);

	String getTranslatedSignature(String signature, boolean isType, int api);

	default Type getTranslatedType(Type type) {
		String descString = type.getDescriptor();
		switch (type.getSort()) {
			case Type.OBJECT: {
				ClassEntry classEntry = new ClassEntry(type.getInternalName());
				return Type.getObjectType(getTranslatedClass(classEntry).getName());
			}
			case Type.ARRAY: {
				TypeDescriptor descriptor = new TypeDescriptor(descString);
				return Type.getType(getTranslatedTypeDesc(descriptor).toString());
			}
			case Type.METHOD: {
				MethodDescriptor descriptor = new MethodDescriptor(descString);
				return Type.getMethodType(getTranslatedMethodDesc(descriptor).toString());
			}
		}
		return type;
	}

	default Handle getTranslatedHandle(Handle handle) {
		MethodEntry entry = new MethodEntry(new ClassEntry(handle.getOwner()), handle.getName(), new MethodDescriptor(handle.getDesc()));
		MethodEntry translatedMethod = getTranslatedMethod(entry);
		ClassEntry ownerClass = translatedMethod.getOwnerClassEntry();
		return new Handle(handle.getTag(), ownerClass.getName(), translatedMethod.getName(), translatedMethod.getDesc().toString(), handle.isInterface());
	}

	default Object getTranslatedValue(Object value) {
		if (value instanceof Type) {
			return this.getTranslatedType((Type) value);
		} else if (value instanceof Handle) {
			return getTranslatedHandle((Handle) value);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	default <T extends Entry> T getTranslatedEntry(T entry) {
		if (entry instanceof ClassDefEntry) {
			return (T) getTranslatedClassDef((ClassDefEntry) entry);
		} else if (entry instanceof ClassEntry) {
			return (T) getTranslatedClass((ClassEntry) entry);
		} else if (entry instanceof FieldDefEntry) {
			return (T) getTranslatedFieldDef((FieldDefEntry) entry);
		} else if (entry instanceof MethodDefEntry) {
			return (T) getTranslatedMethodDef((MethodDefEntry) entry);
		} else if (entry instanceof FieldEntry) {
			return (T) getTranslatedField((FieldEntry) entry);
		} else if (entry instanceof MethodEntry) {
			return (T) getTranslatedMethod((MethodEntry) entry);
		} else if (entry instanceof LocalVariableDefEntry) {
			return (T) getTranslatedVariableDef((LocalVariableDefEntry) entry);
		} else if (entry instanceof LocalVariableEntry) {
			return (T) getTranslatedVariable((LocalVariableEntry) entry);
		} else if (entry instanceof TypeDescriptor) {
			return (T) getTranslatedTypeDesc((TypeDescriptor) entry);
		} else if (entry instanceof MethodDescriptor) {
			return (T) getTranslatedMethodDesc((MethodDescriptor) entry);
		}
		throw new IllegalArgumentException("Cannot translate unknown entry type");
	}
}
