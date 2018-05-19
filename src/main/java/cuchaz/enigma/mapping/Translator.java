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

	TypeDescriptor getTranslatedTypeDesc(TypeDescriptor desc);

	MethodDescriptor getTranslatedMethodDesc(MethodDescriptor descriptor);

	default Type getTranslatedType(Type type) {
		String descString = type.getDescriptor();
		// If this is a method
		if (descString.contains("(")) {
			MethodDescriptor descriptor = new MethodDescriptor(descString);
			return Type.getMethodType(getTranslatedMethodDesc(descriptor).toString());
		} else {
			TypeDescriptor descriptor = new TypeDescriptor(descString);
			return Type.getType(getTranslatedTypeDesc(descriptor).toString());
		}
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
