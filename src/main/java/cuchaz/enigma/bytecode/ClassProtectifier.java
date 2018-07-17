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

package cuchaz.enigma.bytecode;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassProtectifier extends ClassVisitor {

	public ClassProtectifier(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		access = protectify(access);
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		access = protectify(access);
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		access = protectify(access);
		super.visitInnerClass(name, outerName, innerName, access);
	}

	private static int protectify(int access) {
		AccessFlags accessFlags = new AccessFlags(access);
		if (accessFlags.isPrivate()) {
			accessFlags.setProtected();
		}
		return accessFlags.getFlags();
	}
}
