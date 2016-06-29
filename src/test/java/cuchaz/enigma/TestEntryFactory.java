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
package cuchaz.enigma;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Type;

public class TestEntryFactory {
	
	public static ClassEntry newClass(String name) {
		return new ClassEntry(name);
	}
	
	public static FieldEntry newField(String className, String fieldName, String fieldType) {
		return newField(newClass(className), fieldName, fieldType);
	}
	
	public static FieldEntry newField(ClassEntry classEntry, String fieldName, String fieldType) {
		return new FieldEntry(classEntry, fieldName, new Type(fieldType));
	}
	
	public static MethodEntry newMethod(String className, String methodName, String methodSignature) {
		return newMethod(newClass(className), methodName, methodSignature);
	}
	
	public static MethodEntry newMethod(ClassEntry classEntry, String methodName, String methodSignature) {
		return new MethodEntry(classEntry, methodName, new Signature(methodSignature));
	}
	
	public static ConstructorEntry newConstructor(String className, String signature) {
		return newConstructor(newClass(className), signature);
	}
	
	public static ConstructorEntry newConstructor(ClassEntry classEntry, String signature) {
		return new ConstructorEntry(classEntry, new Signature(signature));
	}
	
	public static EntryReference<FieldEntry,BehaviorEntry> newFieldReferenceByMethod(FieldEntry fieldEntry, String callerClassName, String callerName, String callerSignature) {
		return new EntryReference<FieldEntry,BehaviorEntry>(fieldEntry, "", newMethod(callerClassName, callerName, callerSignature));
	}
	
	public static EntryReference<FieldEntry,BehaviorEntry> newFieldReferenceByConstructor(FieldEntry fieldEntry, String callerClassName, String callerSignature) {
		return new EntryReference<FieldEntry,BehaviorEntry>(fieldEntry, "", newConstructor(callerClassName, callerSignature));
	}
	
	public static EntryReference<BehaviorEntry,BehaviorEntry> newBehaviorReferenceByMethod(BehaviorEntry behaviorEntry, String callerClassName, String callerName, String callerSignature) {
		return new EntryReference<BehaviorEntry,BehaviorEntry>(behaviorEntry, "", newMethod(callerClassName, callerName, callerSignature));
	}
	
	public static EntryReference<BehaviorEntry,BehaviorEntry> newBehaviorReferenceByConstructor(BehaviorEntry behaviorEntry, String callerClassName, String callerSignature) {
		return new EntryReference<BehaviorEntry,BehaviorEntry>(behaviorEntry, "", newConstructor(callerClassName, callerSignature));
	}
}
