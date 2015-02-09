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
package cuchaz.enigma.bytecode;

import java.util.Collection;

import javassist.CtClass;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.InnerClassesAttribute;
import cuchaz.enigma.Constants;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;

public class InnerClassWriter {
	
	private JarIndex m_jarIndex;
	
	public InnerClassWriter(JarIndex jarIndex) {
		m_jarIndex = jarIndex;
	}
	
	public void write(CtClass c) {
		
		// is this an inner or outer class?
		String obfInnerClassName = new ClassEntry(Descriptor.toJvmName(c.getName())).getSimpleName();
		String obfOuterClassName = m_jarIndex.getOuterClass(obfInnerClassName);
		if (obfOuterClassName == null) {
			// this is an outer class
			obfOuterClassName = Descriptor.toJvmName(c.getName());
		} else {
			// this is an inner class, rename it to outer$inner
			ClassEntry obfClassEntry = new ClassEntry(obfOuterClassName + "$" + obfInnerClassName);
			c.setName(obfClassEntry.getName());
			
			BehaviorEntry caller = m_jarIndex.getAnonymousClassCaller(obfInnerClassName);
			if (caller != null) {
				// write the enclosing method attribute
				if (caller.getName().equals("<clinit>")) {
					c.getClassFile().addAttribute(new EnclosingMethodAttribute(c.getClassFile().getConstPool(), caller.getClassName()));
				} else {
					c.getClassFile().addAttribute(new EnclosingMethodAttribute(c.getClassFile().getConstPool(), caller.getClassName(), caller.getName(), caller.getSignature().toString()));
				}
			}
		}
		
		// write the inner classes if needed
		Collection<String> obfInnerClassNames = m_jarIndex.getInnerClasses(obfOuterClassName);
		if (obfInnerClassNames != null && !obfInnerClassNames.isEmpty()) {
			writeInnerClasses(c, obfOuterClassName, obfInnerClassNames);
		}
	}
	
	private void writeInnerClasses(CtClass c, String obfOuterClassName, Collection<String> obfInnerClassNames) {
		InnerClassesAttribute attr = new InnerClassesAttribute(c.getClassFile().getConstPool());
		c.getClassFile().addAttribute(attr);
		for (String obfInnerClassName : obfInnerClassNames) {
			// get the new inner class name
			ClassEntry obfClassEntry = new ClassEntry(obfOuterClassName + "$" + obfInnerClassName);
			
			// here's what the JVM spec says about the InnerClasses attribute
			// append( inner, outer of inner if inner is member of outer 0 ow, name after $ if inner not anonymous 0 ow, flags );
			
			// update the attribute with this inner class
			ConstPool constPool = c.getClassFile().getConstPool();
			int innerClassIndex = constPool.addClassInfo(obfClassEntry.getName());
			int outerClassIndex = 0;
			int innerClassSimpleNameIndex = 0;
			if (!m_jarIndex.isAnonymousClass(obfInnerClassName)) {
				outerClassIndex = constPool.addClassInfo(obfClassEntry.getOuterClassName());
				innerClassSimpleNameIndex = constPool.addUtf8Info(obfClassEntry.getInnerClassName());
			}
			
			attr.append(innerClassIndex, outerClassIndex, innerClassSimpleNameIndex, c.getClassFile().getAccessFlags() & ~AccessFlag.SUPER);
			
			/* DEBUG 
			System.out.println(String.format("\tOBF: %s -> ATTR: %s,%s,%s (replace %s with %s)",
				obfClassEntry,
				attr.outerClass(attr.tableLength() - 1),
				attr.innerClass(attr.tableLength() - 1),
				attr.innerName(attr.tableLength() - 1),
				Constants.NonePackage + "/" + obfInnerClassName,
				obfClassEntry.getName()
			));
			*/
			
			// make sure the outer class references only the new inner class names
			c.replaceClassName(Constants.NonePackage + "/" + obfInnerClassName, obfClassEntry.getName());
		}
	}
}
