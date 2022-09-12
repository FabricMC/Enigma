/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
*     Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class TestEntryFactory {
	public static ClassEntry newClass(String name) {
		return new ClassEntry(name);
	}

	public static FieldEntry newField(String className, String fieldName, String fieldType) {
		return newField(newClass(className), fieldName, fieldType);
	}

	public static FieldEntry newField(ClassEntry classEntry, String fieldName, String fieldType) {
		return new FieldEntry(classEntry, fieldName, new TypeDescriptor(fieldType));
	}

	public static MethodEntry newMethod(String className, String methodName, String methodSignature) {
		return newMethod(newClass(className), methodName, methodSignature);
	}

	public static MethodEntry newMethod(ClassEntry classEntry, String methodName, String methodSignature) {
		return new MethodEntry(classEntry, methodName, new MethodDescriptor(methodSignature));
	}

	public static EntryReference<FieldEntry, MethodEntry> newFieldReferenceByMethod(FieldEntry fieldEntry, String callerClassName, String callerName, String callerSignature) {
		return new EntryReference<>(fieldEntry, "", newMethod(callerClassName, callerName, callerSignature));
	}

	public static EntryReference<MethodEntry, MethodEntry> newBehaviorReferenceByMethod(MethodEntry methodEntry, String callerClassName, String callerName, String callerSignature) {
		return new EntryReference<>(methodEntry, "", newMethod(callerClassName, callerName, callerSignature));
	}
}
