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
import javassist.CtMethod;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.SourceFileAttribute;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.Type;

public class ClassTranslator {
	
	private Translator m_translator;
	
	public ClassTranslator(Translator translator) {
		m_translator = translator;
	}
	
	public void translate(CtClass c) {
		
		// NOTE: the order of these translations is very important
		
		// translate all the field and method references in the code by editing the constant pool
		ConstPool constants = c.getClassFile().getConstPool();
		ConstPoolEditor editor = new ConstPoolEditor(constants);
		for (int i = 1; i < constants.getSize(); i++) {
			switch (constants.getTag(i)) {
				
				case ConstPool.CONST_Fieldref: {
					
					// translate the name and type
					FieldEntry entry = EntryFactory.getFieldEntry(
						Descriptor.toJvmName(constants.getFieldrefClassName(i)),
						constants.getFieldrefName(i),
						constants.getFieldrefType(i)
					);
					FieldEntry translatedEntry = m_translator.translateEntry(entry);
					if (!entry.equals(translatedEntry)) {
						editor.changeMemberrefNameAndType(i, translatedEntry.getName(), translatedEntry.getType().toString());
					}
				}
				break;
				
				case ConstPool.CONST_Methodref:
				case ConstPool.CONST_InterfaceMethodref: {
					
					// translate the name and type (ie signature)
					BehaviorEntry entry = EntryFactory.getBehaviorEntry(
						Descriptor.toJvmName(editor.getMemberrefClassname(i)),
						editor.getMemberrefName(i),
						editor.getMemberrefType(i)
					);
					BehaviorEntry translatedEntry = m_translator.translateEntry(entry);
					if (!entry.equals(translatedEntry)) {
						editor.changeMemberrefNameAndType(i, translatedEntry.getName(), translatedEntry.getSignature().toString());
					}
				}
				break;
			}
		}
		
		ClassEntry classEntry = new ClassEntry(Descriptor.toJvmName(c.getName()));
		
		// translate all the fields
		for (CtField field : c.getDeclaredFields()) {
			
			// translate the name
			FieldEntry entry = EntryFactory.getFieldEntry(field);
			String translatedName = m_translator.translate(entry);
			if (translatedName != null) {
				field.setName(translatedName);
			}
			
			// translate the type
			Type translatedType = m_translator.translateType(entry.getType());
			field.getFieldInfo().setDescriptor(translatedType.toString());
		}
		
		// translate all the methods and constructors
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {
			
			BehaviorEntry entry = EntryFactory.getBehaviorEntry(behavior);
			
			if (behavior instanceof CtMethod) {
				CtMethod method = (CtMethod)behavior;
				
				// translate the name
				String translatedName = m_translator.translate(entry);
				if (translatedName != null) {
					method.setName(translatedName);
				}
			}
			
			if (entry.getSignature() != null) {
				// translate the signature
				Signature translatedSignature = m_translator.translateSignature(entry.getSignature());
				behavior.getMethodInfo().setDescriptor(translatedSignature.toString());
			}
		}
		
		// translate the EnclosingMethod attribute
		EnclosingMethodAttribute enclosingMethodAttr = (EnclosingMethodAttribute)c.getClassFile().getAttribute(EnclosingMethodAttribute.tag);
		if (enclosingMethodAttr != null) {
			
			if (enclosingMethodAttr.methodIndex() == 0) {
				BehaviorEntry obfBehaviorEntry = EntryFactory.getBehaviorEntry(Descriptor.toJvmName(enclosingMethodAttr.className()));
				BehaviorEntry deobfBehaviorEntry = m_translator.translateEntry(obfBehaviorEntry);
				c.getClassFile().addAttribute(new EnclosingMethodAttribute(
					constants,
					deobfBehaviorEntry.getClassName()
				));
			} else {
				BehaviorEntry obfBehaviorEntry = EntryFactory.getBehaviorEntry(
					Descriptor.toJvmName(enclosingMethodAttr.className()),
					enclosingMethodAttr.methodName(),
					enclosingMethodAttr.methodDescriptor()
				);
				BehaviorEntry deobfBehaviorEntry = m_translator.translateEntry(obfBehaviorEntry);
				c.getClassFile().addAttribute(new EnclosingMethodAttribute(
					constants,
					deobfBehaviorEntry.getClassName(),
					deobfBehaviorEntry.getName(),
					deobfBehaviorEntry.getSignature().toString()
				));
			}
		}
		
		// translate all the class names referenced in the code
		// the above code only changed method/field/reference names and types, but not the rest of the class references
		ClassRenamer.renameClasses(c, m_translator);
		
		// translate the source file attribute too
		ClassEntry deobfClassEntry = m_translator.translateEntry(classEntry);
		if (deobfClassEntry != null) {
			String sourceFile = Descriptor.toJvmName(deobfClassEntry.getOutermostClassEntry().getSimpleName()) + ".java";
			c.getClassFile().addAttribute(new SourceFileAttribute(constants, sourceFile));
		}
	}
}
