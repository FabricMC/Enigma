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

package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.TypeDescriptor;
import cuchaz.enigma.mapping.entry.*;
import org.objectweb.asm.*;

public class TranslationClassVisitor extends ClassVisitor {
	private final Translator translator;
	private final JarIndex jarIndex;
	private final ReferencedEntryPool entryPool;

	private ClassDefEntry obfClassEntry;

	public TranslationClassVisitor(Translator translator, JarIndex jarIndex, ReferencedEntryPool entryPool, int api, ClassVisitor cv) {
		super(api, cv);
		this.translator = translator;
		this.jarIndex = jarIndex;
		this.entryPool = entryPool;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		obfClassEntry = new ClassDefEntry(name, new AccessFlags(access));
		ClassDefEntry entry = translator.getTranslatedClassDef(obfClassEntry);
		ClassEntry superEntry = translator.getTranslatedClass(entryPool.getClass(superName));
		String[] translatedInterfaces = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			translatedInterfaces[i] = translator.getTranslatedClass(entryPool.getClass(interfaces[i])).getName();
		}
		String translatedSignature = translator.getTranslatedSignature(signature, false, api);
		super.visit(version, entry.getAccess().getFlags(), entry.getName(), translatedSignature, superEntry.getName(), translatedInterfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldDefEntry entry = new FieldDefEntry(obfClassEntry, name, new TypeDescriptor(desc), new AccessFlags(access));
		FieldDefEntry translatedEntry = translator.getTranslatedFieldDef(entry);
		String translatedSignature = translator.getTranslatedSignature(signature, true, api);
		FieldVisitor fv = super.visitField(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedSignature, value);
		return new TranslationFieldVisitor(translator, translatedEntry, api, fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(obfClassEntry, name, new MethodDescriptor(desc), new AccessFlags(access));
		MethodDefEntry translatedEntry = translator.getTranslatedMethodDef(entry);
		if (jarIndex.getBridgedMethod(entry) != null) {
			translatedEntry.getAccess().setBridged();
		}
		String[] translatedExceptions = new String[exceptions.length];
		for (int i = 0; i < exceptions.length; i++) {
			translatedExceptions[i] = translator.getTranslatedClass(entryPool.getClass(exceptions[i])).getName();
		}
		String translatedSignature = translator.getTranslatedSignature(signature, false, api);
		MethodVisitor mv = super.visitMethod(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedSignature, translatedExceptions);
		return new TranslationMethodVisitor(translator, translatedEntry, api, mv);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		if (desc != null) {
			MethodEntry translatedEntry = translator.getTranslatedMethod(new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc)));
			super.visitOuterClass(translatedEntry.getClassName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
		} else {
			super.visitOuterClass(owner, name, desc);
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor translatedDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(translatedDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, translatedDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor translatedDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, translatedDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, translatedDesc.getTypeEntry(), api, av);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		ClassDefEntry translatedEntry = translator.getTranslatedClassDef(new ClassDefEntry(name, new AccessFlags(access)));
		String translatedName = translatedEntry.getName();
		int separatorIndex = translatedName.lastIndexOf("$");
		String parentName = translatedName.substring(0, separatorIndex);
		String childName = translatedName.substring(separatorIndex + 1);

		ClassEntry outerEntry = translator.getTranslatedClass(entryPool.getClass(parentName));
		super.visitInnerClass(translatedName, outerEntry.getName(), childName, translatedEntry.getAccess().getFlags());
	}
}
