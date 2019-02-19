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

package cuchaz.enigma.translation.representation;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import cuchaz.enigma.translation.representation.entry.*;

public class ProcyonEntryFactory {
	public FieldEntry getFieldEntry(MemberReference def) {
		ClassEntry classEntry = new ClassEntry(def.getDeclaringType().getInternalName());
		return new FieldEntry(classEntry, def.getName(), new TypeDescriptor(def.getErasedSignature()));
	}

	public FieldDefEntry getFieldDefEntry(FieldDefinition def) {
		ClassEntry classEntry = new ClassEntry(def.getDeclaringType().getInternalName());
		return new FieldDefEntry(classEntry, def.getName(), new TypeDescriptor(def.getErasedSignature()), Signature.createTypedSignature(def.getSignature()), new AccessFlags(def.getModifiers()));
	}

	public MethodEntry getMethodEntry(MemberReference def) {
		ClassEntry classEntry = new ClassEntry(def.getDeclaringType().getInternalName());
		return new MethodEntry(classEntry, def.getName(), new MethodDescriptor(def.getErasedSignature()));
	}

	public MethodDefEntry getMethodDefEntry(MethodDefinition def) {
		ClassEntry classEntry = new ClassEntry(def.getDeclaringType().getInternalName());
		return new MethodDefEntry(classEntry, def.getName(), new MethodDescriptor(def.getErasedSignature()), Signature.createSignature(def.getSignature()), new AccessFlags(def.getModifiers()));
	}
}
