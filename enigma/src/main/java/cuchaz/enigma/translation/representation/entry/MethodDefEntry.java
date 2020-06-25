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

package cuchaz.enigma.translation.representation.entry;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;

public class MethodDefEntry extends MethodEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public MethodDefEntry(ClassEntry owner, String name, MethodDescriptor descriptor, Signature signature, AccessFlags access) {
		this(owner, name, descriptor, signature, access, null);
	}

	public MethodDefEntry(ClassEntry owner, String name, MethodDescriptor descriptor, Signature signature, AccessFlags access, String docs) {
		super(owner, name, descriptor, docs);
		Preconditions.checkNotNull(access, "Method access cannot be null");
		Preconditions.checkNotNull(signature, "Method signature cannot be null");
		this.access = access;
		this.signature = signature;
	}

	public static MethodDefEntry parse(ClassEntry owner, int access, String name, String desc, String signature) {
		return new MethodDefEntry(owner, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access), null);
	}

	@Override
	public AccessFlags getAccess() {
		return access;
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	protected TranslateResult<MethodDefEntry> extendedTranslate(Translator translator, @Nullable EntryMapping mapping) {
		MethodDescriptor translatedDesc = translator.translate(descriptor);
		Signature translatedSignature = translator.translate(signature);
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		AccessFlags translatedAccess = mapping != null ? mapping.getAccessModifier().transform(access) : access;
		String docs = mapping != null ? mapping.getJavadoc() : null;
		return TranslateResult.of(
				mapping == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new MethodDefEntry(parent, translatedName, translatedDesc, translatedSignature, translatedAccess, docs)
		);
	}

	@Override
	public MethodDefEntry withName(String name) {
		return new MethodDefEntry(parent, name, descriptor, signature, access, javadocs);
	}

	@Override
	public MethodDefEntry withParent(ClassEntry parent) {
		return new MethodDefEntry(new ClassEntry(parent.getFullName()), name, descriptor, signature, access, javadocs);
	}
}
