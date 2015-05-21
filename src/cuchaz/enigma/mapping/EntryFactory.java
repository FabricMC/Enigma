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

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.expr.ConstructorCall;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import cuchaz.enigma.analysis.JarIndex;

public class EntryFactory {
	
	public static ClassEntry getClassEntry(CtClass c) {
		return new ClassEntry(Descriptor.toJvmName(c.getName()));
	}
	
	public static ClassEntry getObfClassEntry(JarIndex jarIndex, ClassMapping classMapping) {
		ClassEntry obfClassEntry = new ClassEntry(classMapping.getObfFullName());
		return obfClassEntry.buildClassEntry(jarIndex.getObfClassChain(obfClassEntry));
	}
	
	private static ClassEntry getObfClassEntry(ClassMapping classMapping) {
		return new ClassEntry(classMapping.getObfFullName());
	}
	
	public static ClassEntry getDeobfClassEntry(ClassMapping classMapping) {
		return new ClassEntry(classMapping.getDeobfName());
	}
	
	public static ClassEntry getSuperclassEntry(CtClass c) {
		return new ClassEntry(Descriptor.toJvmName(c.getClassFile().getSuperclass()));
	}
	
	public static FieldEntry getFieldEntry(CtField field) {
		return new FieldEntry(
			getClassEntry(field.getDeclaringClass()),
			field.getName(),
			new Type(field.getFieldInfo().getDescriptor())
		);
	}
	
	public static FieldEntry getFieldEntry(FieldAccess call) {
		return new FieldEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			call.getFieldName(),
			new Type(call.getSignature())
		);
	}
	
	public static FieldEntry getFieldEntry(String className, String name, String type) {
		return new FieldEntry(new ClassEntry(className), name, new Type(type));
	}
	
	public static FieldEntry getObfFieldEntry(ClassMapping classMapping, FieldMapping fieldMapping) {
		return new FieldEntry(
			getObfClassEntry(classMapping),
			fieldMapping.getObfName(),
			fieldMapping.getObfType()
		);
	}
	
	public static MethodEntry getMethodEntry(CtMethod method) {
		return new MethodEntry(
			getClassEntry(method.getDeclaringClass()),
			method.getName(),
			new Signature(method.getMethodInfo().getDescriptor())
		);
	}
	
	public static MethodEntry getMethodEntry(MethodCall call) {
		return new MethodEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			call.getMethodName(),
			new Signature(call.getSignature())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(CtConstructor constructor) {
		if (constructor.isClassInitializer()) {
			return new ConstructorEntry(
				getClassEntry(constructor.getDeclaringClass())
			);
		} else {
			return new ConstructorEntry(
				getClassEntry(constructor.getDeclaringClass()),
				new Signature(constructor.getMethodInfo().getDescriptor())
			);
		}
	}
	
	public static ConstructorEntry getConstructorEntry(ConstructorCall call) {
		return new ConstructorEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			new Signature(call.getSignature())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(NewExpr call) {
		return new ConstructorEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			new Signature(call.getSignature())
		);
	}
	
	public static BehaviorEntry getBehaviorEntry(CtBehavior behavior) {
		if (behavior instanceof CtMethod) {
			return getMethodEntry((CtMethod)behavior);
		} else if (behavior instanceof CtConstructor) {
			return getConstructorEntry((CtConstructor)behavior);
		}
		throw new Error("behavior is neither Method nor Constructor!");
	}
	
	public static BehaviorEntry getBehaviorEntry(String className, String behaviorName, String behaviorSignature) {
		return getBehaviorEntry(new ClassEntry(className), behaviorName, new Signature(behaviorSignature));
	}

	public static BehaviorEntry getBehaviorEntry(String className, String behaviorName) {
		return getBehaviorEntry(new ClassEntry(className), behaviorName);
	}
	
	public static BehaviorEntry getBehaviorEntry(String className) {
		return new ConstructorEntry(new ClassEntry(className));
	}
	
	public static BehaviorEntry getBehaviorEntry(ClassEntry classEntry, String behaviorName, Signature behaviorSignature) {
		if (behaviorName.equals("<init>")) {
			return new ConstructorEntry(classEntry, behaviorSignature);
		} else if(behaviorName.equals("<clinit>")) {
			return new ConstructorEntry(classEntry);
		} else {
			return new MethodEntry(classEntry, behaviorName, behaviorSignature);
		}
	}
	
	public static BehaviorEntry getBehaviorEntry(ClassEntry classEntry, String behaviorName) {
		if(behaviorName.equals("<clinit>")) {
			return new ConstructorEntry(classEntry);
		} else {
			throw new IllegalArgumentException("Only class initializers don't have signatures");
		}
	}
	
	public static BehaviorEntry getObfBehaviorEntry(ClassEntry classEntry, MethodMapping methodMapping) {
		return getBehaviorEntry(classEntry, methodMapping.getObfName(), methodMapping.getObfSignature());
	}
	
	public static BehaviorEntry getObfBehaviorEntry(ClassMapping classMapping, MethodMapping methodMapping) {
		return getObfBehaviorEntry(getObfClassEntry(classMapping), methodMapping);
	}
}
