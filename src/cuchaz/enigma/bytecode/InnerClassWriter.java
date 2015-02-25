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
import java.util.Collections;
import java.util.List;

import javassist.CtClass;
import javassist.bytecode.ConstPool;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.InnerClassesAttribute;

import com.beust.jcommander.internal.Lists;

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
		
		// build the class chain (inner to outer)
		ClassEntry obfClassEntry = EntryFactory.getClassEntry(c);
		List<ClassEntry> obfClassChain = Lists.newArrayList();
		ClassEntry checkClassEntry = obfClassEntry;
		while (checkClassEntry != null) {
			obfClassChain.add(checkClassEntry);
			checkClassEntry = m_index.getOuterClass(checkClassEntry);
		}
		
		// change order: outer to inner
		Collections.reverse(obfClassChain);
		
		// does this class have any inner classes?
		Collection<ClassEntry> obfInnerClassEntries = m_index.getInnerClasses(obfClassEntry);
		
		boolean isInnerClass = obfClassChain.size() > 1;
		if (isInnerClass) {
			
			// it's an inner class, rename it to the fully qualified name
			StringBuilder buf = new StringBuilder();
			for (ClassEntry obfChainClassEntry : obfClassChain) {
				if (buf.length() == 0) {
					buf.append(obfChainClassEntry.getName());
				} else {
					buf.append("$");
					buf.append(obfChainClassEntry.getSimpleName());
				}
			}
			c.setName(buf.toString());
			
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
		
		if (isInnerClass || !obfInnerClassEntries.isEmpty()) {
			
			// create an inner class attribute
			InnerClassesAttribute attr = new InnerClassesAttribute(c.getClassFile().getConstPool());
			c.getClassFile().addAttribute(attr);
			
			// write the ancestry, but not the outermost class
			for (int i=1; i<obfClassChain.size(); i++) {
				writeInnerClass(attr, obfClassChain, obfClassChain.get(i));
			}
			
			// write the inner classes
			for (ClassEntry obfInnerClassEntry : obfInnerClassEntries) {
				
				// extend the class chain
				List<ClassEntry> extendedObfClassChain = Lists.newArrayList(obfClassChain);
				extendedObfClassChain.add(obfInnerClassEntry);
				
				String fullyQualifiedInnerClassName = writeInnerClass(attr, extendedObfClassChain, obfInnerClassEntry);
				
				// make sure we only reference the fully qualified inner class name
				c.replaceClassName(obfInnerClassEntry.getName(), fullyQualifiedInnerClassName);
			}
		}
	}
	
	private String writeInnerClass(InnerClassesAttribute attr, List<ClassEntry> obfClassChain, ClassEntry obfClassEntry) {
		
		// get the new inner class name
		String obfInnerClassName = getFullyQualifiedName(obfClassChain, obfClassEntry);
		String obfParentClassName = getFullyQualifiedParentName(obfClassChain, obfClassEntry);
		
		// here's what the JVM spec says about the InnerClasses attribute
		// append(inner, parent, 0 if anonymous else simple name, flags);
		
		// update the attribute with this inner class
		ConstPool constPool = attr.getConstPool();
		int innerClassIndex = constPool.addClassInfo(obfInnerClassName);
		int parentClassIndex = constPool.addClassInfo(obfParentClassName);
		int innerClassSimpleNameIndex = 0;
		int accessFlags = 0;
		if (!m_index.isAnonymousClass(obfClassEntry)) {
			innerClassSimpleNameIndex = constPool.addUtf8Info(obfClassEntry.getSimpleName());
		}
		
		attr.append(innerClassIndex, parentClassIndex, innerClassSimpleNameIndex, accessFlags);
		
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
		
		return obfInnerClassName;
	}

	private String getFullyQualifiedParentName(List<ClassEntry> classChain, ClassEntry classEntry) {
		assert(classChain.size() > 1);
		assert(classChain.contains(classEntry));
		StringBuilder buf = new StringBuilder();
		for (int i=0; classChain.get(i) != classEntry; i++) {
			ClassEntry chainEntry = classChain.get(i);
			if (buf.length() == 0) {
				buf.append(chainEntry.getName());
			} else {
				buf.append("$");
				buf.append(chainEntry.getSimpleName());
			}
		}
		return buf.toString();
	}
	
	private String getFullyQualifiedName(List<ClassEntry> classChain, ClassEntry classEntry) {
		boolean isInner = classChain.size() > 1;
		if (isInner) {
			return getFullyQualifiedParentName(classChain, classEntry) + "$" + classEntry.getSimpleName();
		} else {
			return classEntry.getName();
		}
	}
}
