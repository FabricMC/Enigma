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

import javassist.CtBehavior;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

public class BehaviorEntryFactory {
	
	public static BehaviorEntry create(String className, String name, String signature) {
		return create(new ClassEntry(className), name, signature);
	}
	
	public static BehaviorEntry create(ClassEntry classEntry, String name, String signature) {
		if (name.equals("<init>")) {
			return new ConstructorEntry(classEntry, new Signature(signature));
		} else if (name.equals("<clinit>")) {
			return new ConstructorEntry(classEntry);
		} else {
			return new MethodEntry(classEntry, name, new Signature(signature));
		}
	}
	
	public static BehaviorEntry create(CtBehavior behavior) {
		String className = Descriptor.toJvmName(behavior.getDeclaringClass().getName());
		if (behavior instanceof CtMethod) {
			return create(className, behavior.getName(), behavior.getSignature());
		} else if (behavior instanceof CtConstructor) {
			CtConstructor constructor = (CtConstructor)behavior;
			if (constructor.isClassInitializer()) {
				return create(className, "<clinit>", null);
			} else {
				return create(className, "<init>", constructor.getSignature());
			}
		} else {
			throw new IllegalArgumentException("Unable to create BehaviorEntry from " + behavior);
		}
	}
	
	public static BehaviorEntry createObf(ClassEntry classEntry, MethodMapping methodMapping) {
		return create(classEntry, methodMapping.getObfName(), methodMapping.getObfSignature().toString());
	}
	
	public static BehaviorEntry createDeobf(ClassEntry classEntry, MethodMapping methodMapping) {
		return create(classEntry, methodMapping.getDeobfName(), methodMapping.getObfSignature().toString());
	}
}
