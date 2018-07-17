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

public class ClassPublifier extends ClassVisitor {

	public ClassPublifier(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		access = publify(access);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		access = publify(access);
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		access = publify(access);
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		access = publify(access);
		super.visitInnerClass(name, outerName, innerName, access);
	}

	private static int publify(int access) {
		AccessFlags accessFlags = new AccessFlags(access);
		if (!accessFlags.isPublic()) {
			accessFlags.setPublic();
		}
		return accessFlags.getFlags();
	}
}
