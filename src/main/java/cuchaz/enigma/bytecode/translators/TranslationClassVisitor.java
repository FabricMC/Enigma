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
import cuchaz.enigma.mapping.*;
import org.objectweb.asm.*;

import java.util.regex.Pattern;

public class TranslationClassVisitor extends ClassVisitor {
	private static final Pattern OBJECT_PATTERN = Pattern.compile(".*:Ljava/lang/Object;:.*");

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
		if (signature != null && OBJECT_PATTERN.matcher(signature).matches()) {
			signature = signature.replaceAll(":Ljava/lang/Object;:", "::");
		}
		obfClassEntry = new ClassDefEntry(name, new AccessFlags(access));
		ClassDefEntry entry = translator.getTranslatedClassDef(obfClassEntry);
		ClassEntry superEntry = translator.getTranslatedClass(entryPool.getClass(superName));
		String[] translatedInterfaces = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			translatedInterfaces[i] = translator.getTranslatedClass(entryPool.getClass(interfaces[i])).getName();
		}
		super.visit(version, entry.getAccess().getFlags(), entry.getName(), signature, superEntry.getName(), translatedInterfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldDefEntry entry = new FieldDefEntry(obfClassEntry, name, new TypeDescriptor(desc), new AccessFlags(access));
		FieldDefEntry translatedEntry = translator.getTranslatedFieldDef(entry);
		return super.visitField(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(obfClassEntry, name, new MethodDescriptor(desc), new AccessFlags(access));
		MethodDefEntry translatedEntry = translator.getTranslatedMethodDef(entry);
		if (jarIndex.getBridgedMethod(entry) != null) {
			translatedEntry.getAccess().setBridged();
		}
		MethodVisitor mv = super.visitMethod(translatedEntry.getAccess().getFlags(), name, desc, signature, exceptions);
		return new TranslationMethodVisitor(translator, translatedEntry, api, mv);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		if (name != null) {
			ClassEntry ownerEntry = translator.getTranslatedClass(entryPool.getClass(owner));
			ClassEntry entry = translator.getTranslatedClass(entryPool.getClass(name));
			String translatedDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc)).toString();
			super.visitOuterClass(ownerEntry.getName(), entry.getName(), translatedDesc);
		} else {
			super.visitOuterClass(owner, name, desc);
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// TODO: Implement
		return super.visitAnnotation(desc, visible);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		// TODO: Implement
		return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// If this is not an anonymous class
		if (innerName != null && outerName != null) {
			ClassDefEntry translatedEntry = translator.getTranslatedClassDef(new ClassDefEntry(innerName, new AccessFlags(access)));
			ClassEntry outerEntry = translator.getTranslatedClass(entryPool.getClass(outerName));
			ClassEntry innerEntry = translator.getTranslatedClass(entryPool.getClass(innerName));
			super.visitInnerClass(translatedEntry.getName(), outerEntry.getName(), innerEntry.getName(), translatedEntry.getAccess().getFlags());
		} else {
			super.visitInnerClass(name, outerName, innerName, access);
		}
	}
}
