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

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import javassist.CtClass;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ConstPool;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.InnerClassesAttribute;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.EntryFactory;

public class InnerClassWriter {
	
	private JarIndex m_index;
	
	public InnerClassWriter(JarIndex index) {
		m_index = index;
	}
	
	public void write(CtClass c) {
		
		// don't change anything if there's already an attribute there
		InnerClassesAttribute oldAttr = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (oldAttr != null) {
			// bail!
			return;
		}
		
		ClassEntry obfClassEntry = EntryFactory.getClassEntry(c);
		List<ClassEntry> obfClassChain = m_index.getObfClassChain(obfClassEntry);
		
		boolean isInnerClass = obfClassChain.size() > 1;
		if (isInnerClass) {
			
			// it's an inner class, rename it to the fully qualified name
			c.setName(obfClassEntry.buildClassEntry(obfClassChain).getName());
			
			BehaviorEntry caller = m_index.getAnonymousClassCaller(obfClassEntry);
			if (caller != null) {
				
				// write the enclosing method attribute
				if (caller.getName().equals("<clinit>")) {
					c.getClassFile().addAttribute(new EnclosingMethodAttribute(c.getClassFile().getConstPool(), caller.getClassName()));
				} else {
					c.getClassFile().addAttribute(new EnclosingMethodAttribute(c.getClassFile().getConstPool(), caller.getClassName(), caller.getName(), caller.getSignature().toString()));
				}
			}
		}
		
		// does this class have any inner classes?
		Collection<ClassEntry> obfInnerClassEntries = m_index.getInnerClasses(obfClassEntry);
		
		if (isInnerClass || !obfInnerClassEntries.isEmpty()) {
			
			// create an inner class attribute
			InnerClassesAttribute attr = new InnerClassesAttribute(c.getClassFile().getConstPool());
			c.getClassFile().addAttribute(attr);
			
			// write the ancestry, but not the outermost class
			for (int i=1; i<obfClassChain.size(); i++) {
				ClassEntry obfInnerClassEntry = obfClassChain.get(i);
				writeInnerClass(attr, obfClassChain, obfInnerClassEntry);
				
				// update references to use the fully qualified inner class name
				c.replaceClassName(obfInnerClassEntry.getName(), obfInnerClassEntry.buildClassEntry(obfClassChain).getName());
			}
			
			// write the inner classes
			for (ClassEntry obfInnerClassEntry : obfInnerClassEntries) {
				
				// extend the class chain
				List<ClassEntry> extendedObfClassChain = Lists.newArrayList(obfClassChain);
				extendedObfClassChain.add(obfInnerClassEntry);
				
				writeInnerClass(attr, extendedObfClassChain, obfInnerClassEntry);
				
				// update references to use the fully qualified inner class name
				c.replaceClassName(obfInnerClassEntry.getName(), obfInnerClassEntry.buildClassEntry(extendedObfClassChain).getName());
			}
		}
	}
	
	private void writeInnerClass(InnerClassesAttribute attr, List<ClassEntry> obfClassChain, ClassEntry obfClassEntry) {
		
		// get the new inner class name
		ClassEntry obfInnerClassEntry = obfClassEntry.buildClassEntry(obfClassChain);
		ClassEntry obfOuterClassEntry = obfInnerClassEntry.getOuterClassEntry();
		
		// here's what the JVM spec says about the InnerClasses attribute
		// append(inner, parent, 0 if anonymous else simple name, flags);
		
		// update the attribute with this inner class
		ConstPool constPool = attr.getConstPool();
		int innerClassIndex = constPool.addClassInfo(obfInnerClassEntry.getName());
		int parentClassIndex = constPool.addClassInfo(obfOuterClassEntry.getName());
		int innerClassNameIndex = 0;
		int accessFlags = AccessFlag.PUBLIC;
		// TODO: need to figure out if we can put static or not
		if (!m_index.isAnonymousClass(obfClassEntry)) {
			innerClassNameIndex = constPool.addUtf8Info(obfInnerClassEntry.getInnermostClassName());
		}
		
		attr.append(innerClassIndex, parentClassIndex, innerClassNameIndex, accessFlags);
		
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
	}
}
