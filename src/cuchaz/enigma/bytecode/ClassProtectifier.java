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
package cuchaz.enigma.bytecode;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.InnerClassesAttribute;


public class ClassProtectifier {

	public static CtClass protectify(CtClass c) {
		
		// protectify all the fields
		for (CtField field : c.getDeclaredFields()) {
			field.setModifiers(protectify(field.getModifiers()));
		}
		
		// protectify all the methods and constructors
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {
			behavior.setModifiers(protectify(behavior.getModifiers()));
		}
		
		// protectify all the inner classes
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (attr != null) {
			for (int i=0; i<attr.tableLength(); i++) {
				attr.setAccessFlags(i, protectify(attr.accessFlags(i)));
			}
		}
		
		return c;
	}

	private static int protectify(int flags) {
		if (AccessFlag.isPrivate(flags)) {
			flags = AccessFlag.setProtected(flags);
		}
		return flags;
	}
}
