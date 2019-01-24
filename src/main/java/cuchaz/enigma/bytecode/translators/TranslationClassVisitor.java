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

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.ReferencedEntryPool;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;
import org.objectweb.asm.*;

import java.util.Arrays;

public class TranslationClassVisitor extends ClassVisitor {
	private final Translator translator;
	private final ReferencedEntryPool entryPool;

	private ClassDefEntry obfClassEntry;

	public TranslationClassVisitor(Translator translator, ReferencedEntryPool entryPool, int api, ClassVisitor cv) {
		super(api, cv);
		this.translator = translator;
		this.entryPool = entryPool;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		obfClassEntry = ClassDefEntry.parse(access, name, signature, superName, interfaces);

		ClassDefEntry translatedEntry = translator.translate(obfClassEntry);
		String translatedSuper = translatedEntry.getSuperClass() != null ? translatedEntry.getSuperClass().getFullName() : null;
		String[] translatedInterfaces = Arrays.stream(translatedEntry.getInterfaces()).map(ClassEntry::getFullName).toArray(String[]::new);

		super.visit(version, translatedEntry.getAccess().getFlags(), translatedEntry.getFullName(), translatedEntry.getSignature().toString(), translatedSuper, translatedInterfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldDefEntry entry = FieldDefEntry.parse(obfClassEntry, access, name, desc, signature);
		FieldDefEntry translatedEntry = translator.translate(entry);
		FieldVisitor fv = super.visitField(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedEntry.getSignature().toString(), value);
		return new TranslationFieldVisitor(translator, translatedEntry, api, fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = MethodDefEntry.parse(obfClassEntry, access, name, desc, signature);
		MethodDefEntry translatedEntry = translator.translate(entry);
		String[] translatedExceptions = new String[exceptions.length];
		for (int i = 0; i < exceptions.length; i++) {
			translatedExceptions[i] = translator.translate(entryPool.getClass(exceptions[i])).getFullName();
		}
		MethodVisitor mv = super.visitMethod(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedEntry.getSignature().toString(), translatedExceptions);
		return new TranslationMethodVisitor(translator, obfClassEntry, entry, api, mv);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		ClassDefEntry classEntry = ClassDefEntry.parse(access, name, obfClassEntry.getSignature().toString(), null, new String[0]);
		ClassDefEntry translatedEntry = translator.translate(classEntry);
		ClassEntry translatedOuterClass = translatedEntry.getOuterClass();
		if (translatedOuterClass == null) {
			throw new IllegalStateException("Translated inner class did not have outer class");
		}

		// Anonymous classes do not specify an outer or inner name. As we do not translate from the given parameter, ignore if the input is null
		String translatedName = translatedEntry.getFullName();
		String translatedOuterName = outerName != null ? translatedOuterClass.getFullName() : null;
		String translatedInnerName = innerName != null ? translatedEntry.getName() : null;
		super.visitInnerClass(translatedName, translatedOuterName, translatedInnerName, translatedEntry.getAccess().getFlags());
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		if (desc != null) {
			MethodEntry translatedEntry = translator.translate(new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc)));
			super.visitOuterClass(translatedEntry.getParent().getFullName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
		} else {
			super.visitOuterClass(owner, name, desc);
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor translatedDesc = translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(translatedDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, translatedDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor translatedDesc = translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, translatedDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, translatedDesc.getTypeEntry(), api, av);
	}
}
