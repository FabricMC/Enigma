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

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;


public class ProcyonEntryFactory {
	
	public static FieldEntry getFieldEntry(FieldDefinition def) {
		return new FieldEntry(
			new ClassEntry(def.getDeclaringType().getInternalName()),
			def.getName(),
			new Type(def.getErasedSignature())
		);
	}
	
	public static MethodEntry getMethodEntry(MethodDefinition def) {
		return new MethodEntry(
			new ClassEntry(def.getDeclaringType().getInternalName()),
			def.getName(),
			new Signature(def.getErasedSignature())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(MethodDefinition def) {
		if (def.isTypeInitializer()) {
			return new ConstructorEntry(
				new ClassEntry(def.getDeclaringType().getInternalName())
			);
		} else {
			return new ConstructorEntry(
				new ClassEntry(def.getDeclaringType().getInternalName()),
				new Signature(def.getErasedSignature())
			);
		}
	}
	
	public static BehaviorEntry getBehaviorEntry(MethodDefinition def) {
		if (def.isConstructor() || def.isTypeInitializer()) {
			return getConstructorEntry(def);
		} else {
			return getMethodEntry(def);
		}
	}
}
