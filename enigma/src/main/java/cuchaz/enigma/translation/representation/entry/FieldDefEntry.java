/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation.representation.entry;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.TypeDescriptor;

public class FieldDefEntry extends FieldEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public FieldDefEntry(ClassEntry owner, String name, TypeDescriptor desc, Signature signature, AccessFlags access) {
		this(owner, name, desc, signature, access, null);
	}

	public FieldDefEntry(ClassEntry owner, String name, TypeDescriptor desc, Signature signature, AccessFlags access, String javadocs) {
		super(owner, name, desc, javadocs);
		this.access = Objects.requireNonNull(access, "Field access cannot be null");
		this.signature = Objects.requireNonNull(signature, "Field signature cannot be null");
	}

	public static FieldDefEntry parse(ClassEntry owner, int access, String name, String desc, String signature) {
		return new FieldDefEntry(owner, name, new TypeDescriptor(desc), Signature.createTypedSignature(signature), new AccessFlags(access), null);
	}

	@Override
	public AccessFlags getAccess() {
		return access;
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	protected TranslateResult<FieldEntry> extendedTranslate(Translator translator, @NotNull EntryMapping mapping) {
		TypeDescriptor translatedDesc = translator.translate(desc);
		Signature translatedSignature = translator.translate(signature);
		String translatedName = mapping.targetName() != null ? mapping.targetName() : name;
		AccessFlags translatedAccess = mapping.accessModifier().transform(access);
		String docs = mapping.javadoc();
		return TranslateResult.of(mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED, new FieldDefEntry(parent, translatedName, translatedDesc, translatedSignature, translatedAccess, docs));
	}

	@Override
	public FieldDefEntry withName(String name) {
		return new FieldDefEntry(parent, name, desc, signature, access, javadocs);
	}

	@Override
	public FieldDefEntry withParent(ClassEntry owner) {
		return new FieldDefEntry(owner, this.name, this.desc, signature, access, javadocs);
	}
}
