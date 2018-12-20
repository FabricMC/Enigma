package cuchaz.enigma.translation.representation.entry;

import com.google.common.base.Preconditions;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.TypeDescriptor;

import javax.annotation.Nullable;

/**
 * TypeDescriptor...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableDefEntry extends LocalVariableEntry {
	protected final TypeDescriptor desc;

	public LocalVariableDefEntry(MethodEntry ownerEntry, int index, String name, boolean parameter, TypeDescriptor desc) {
		super(ownerEntry, index, name, parameter);
		Preconditions.checkNotNull(desc, "Variable desc cannot be null");

		this.desc = desc;
	}

	public TypeDescriptor getDesc() {
		return desc;
	}

	@Override
	public LocalVariableDefEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		TypeDescriptor translatedDesc = translator.translate(desc);
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		return new LocalVariableDefEntry(parent, index, translatedName, parameter, translatedDesc);
	}

	@Override
	public LocalVariableDefEntry withParent(MethodEntry entry) {
		return new LocalVariableDefEntry(entry, index, name, parameter, desc);
	}

	@Override
	public String toString() {
		return this.parent + "(" + this.index + ":" + this.name + ":" + this.desc + ")";
	}
}
