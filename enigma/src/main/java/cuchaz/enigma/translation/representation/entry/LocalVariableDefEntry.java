package cuchaz.enigma.translation.representation.entry;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.TypeDescriptor;

/**
 * TypeDescriptor...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableDefEntry extends LocalVariableEntry {
	protected final TypeDescriptor desc;

	public LocalVariableDefEntry(MethodEntry ownerEntry, int index, String name, boolean parameter, TypeDescriptor desc, String javadoc) {
		super(ownerEntry, index, name, parameter, javadoc);
		Preconditions.checkNotNull(desc, "Variable desc cannot be null");

		this.desc = desc;
	}

	public TypeDescriptor getDesc() {
		return desc;
	}

	@Override
	protected TranslateResult<LocalVariableEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		TypeDescriptor translatedDesc = translator.translate(desc);
		String translatedName = mapping.targetName() != null ? mapping.targetName() : name;
		String javadoc = mapping.javadoc();
		return TranslateResult.of(mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED, new LocalVariableDefEntry(parent, index, translatedName, parameter, translatedDesc, javadoc));
	}

	@Override
	public LocalVariableDefEntry withName(String name) {
		return new LocalVariableDefEntry(parent, index, name, parameter, desc, javadocs);
	}

	@Override
	public LocalVariableDefEntry withParent(MethodEntry entry) {
		return new LocalVariableDefEntry(entry, index, name, parameter, desc, javadocs);
	}

	@Override
	public String toString() {
		return this.parent + "(" + this.index + ":" + this.name + ":" + this.desc + ")";
	}
}
