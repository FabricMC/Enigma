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


public class ClassPublifier {

	public static CtClass publify(CtClass c) {
		
		// publify all the fields
		for (CtField field : c.getDeclaredFields()) {
			field.setModifiers(publify(field.getModifiers()));
		}
		
		// publify all the methods and constructors
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {
			behavior.setModifiers(publify(behavior.getModifiers()));
		}
		
		// publify all the inner classes
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (attr != null) {
			for (int i=0; i<attr.tableLength(); i++) {
				attr.setAccessFlags(i, publify(attr.accessFlags(i)));
			}
		}
		
		return c;
	}

	private static int publify(int flags) {
		if (AccessFlag.isPrivate(flags) || AccessFlag.isProtected(flags)) {
			flags = AccessFlag.setPublic(flags);
		}
		return flags;
	}
}
