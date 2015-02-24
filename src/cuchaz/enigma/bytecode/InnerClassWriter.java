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
import javassist.bytecode.ConstPool;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.InnerClassesAttribute;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.EntryFactory;

public class InnerClassWriter {
	
	private JarIndex m_jarIndex;
	
	public InnerClassWriter(JarIndex jarIndex) {
		m_jarIndex = jarIndex;
	}
	
	public void write(CtClass c) {
		
		// first, assume this is an inner class
		ClassEntry obfInnerClassEntry = EntryFactory.getClassEntry(c);
		ClassEntry obfOuterClassEntry = m_jarIndex.getOuterClass(obfInnerClassEntry);
		
		// see if we're right
		if (obfOuterClassEntry == null) {
			
			// nope, it's an outer class
			obfInnerClassEntry = null;
			obfOuterClassEntry = EntryFactory.getClassEntry(c);
		} else {
			
			// yeah, it's an inner class, rename it to outer$inner
			ClassEntry obfClassEntry = new ClassEntry(obfOuterClassEntry.getName() + "$" + obfInnerClassEntry.getSimpleName());
			c.setName(obfClassEntry.getName());
			
			BehaviorEntry caller = m_jarIndex.getAnonymousClassCaller(obfInnerClassEntry);
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
		Collection<ClassEntry> obfInnerClassEntries = m_jarIndex.getInnerClasses(obfOuterClassEntry);
		if (obfInnerClassEntries != null && !obfInnerClassEntries.isEmpty()) {
			writeInnerClasses(c, obfOuterClassEntry, obfInnerClassEntries);
		}
	}
	
	private void writeInnerClasses(CtClass c, ClassEntry obfOuterClassEntry, Collection<ClassEntry> obfInnerClassEntries) {
		InnerClassesAttribute attr = new InnerClassesAttribute(c.getClassFile().getConstPool());
		c.getClassFile().addAttribute(attr);
		for (ClassEntry obfInnerClassEntry : obfInnerClassEntries) {
			
			// get the new inner class name
			ClassEntry obfClassEntry = new ClassEntry(obfOuterClassEntry.getName() + "$" + obfInnerClassEntry.getSimpleName());
			
			// here's what the JVM spec says about the InnerClasses attribute
			// append( inner, outer of inner if inner is member of outer 0 ow, name after $ if inner not anonymous 0 ow, flags );
			
			// update the attribute with this inner class
			ConstPool constPool = c.getClassFile().getConstPool();
			int innerClassIndex = constPool.addClassInfo(obfClassEntry.getName());
			int outerClassIndex = 0;
			int innerClassSimpleNameIndex = 0;
			int accessFlags = 0;
			if (!m_jarIndex.isAnonymousClass(obfInnerClassEntry)) {
				outerClassIndex = constPool.addClassInfo(obfClassEntry.getOuterClassName());
				innerClassSimpleNameIndex = constPool.addUtf8Info(obfClassEntry.getInnerClassName());
			}
			
			attr.append(innerClassIndex, outerClassIndex, innerClassSimpleNameIndex, accessFlags);
			
			/* DEBUG 
			System.out.println(String.format("\tOBF: %s -> ATTR: %s,%s,%s (replace %s with %s)",
				obfClassEntry,
				attr.innerClass(attr.tableLength() - 1),
				attr.outerClass(attr.tableLength() - 1),
				attr.innerName(attr.tableLength() - 1),
				Constants.NonePackage + "/" + obfInnerClassName,
				obfClassEntry.getName()
			));
			*/
			
			// make sure the outer class references only the new inner class names
			c.replaceClassName(obfInnerClassEntry.getName(), obfClassEntry.getName());
		}
	}
}
