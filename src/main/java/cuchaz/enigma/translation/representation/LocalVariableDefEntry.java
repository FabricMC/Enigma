package cuchaz.enigma.translation.representation;

import com.google.common.base.Preconditions;
import cuchaz.enigma.utils.Utils;

/**
 * TypeDescriptor...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableDefEntry extends LocalVariableEntry {

	protected final MethodDefEntry ownerEntry;
	protected final TypeDescriptor desc;

	public LocalVariableDefEntry(MethodDefEntry ownerEntry, int index, String name, TypeDescriptor desc) {
		this(ownerEntry, index, name, true, desc);
	}

	public LocalVariableDefEntry(MethodDefEntry ownerEntry, int index, String name, boolean parameter, TypeDescriptor desc) {
		super(ownerEntry, index, name, parameter);
		Preconditions.checkNotNull(desc, "Variable desc cannot be null");

		this.ownerEntry = ownerEntry;
		this.desc = desc;
	}

	@Override
	public MethodDefEntry getOwnerEntry() {
		return this.ownerEntry;
	}

	public TypeDescriptor getDesc() {
		return desc;
	}

	@Override
	public LocalVariableDefEntry updateOwnership(ClassEntry classEntry) {
		return new LocalVariableDefEntry(ownerEntry.updateOwnership(classEntry), index, name, parameter, desc);
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.ownerEntry, this.desc.hashCode(), this.name.hashCode(), Integer.hashCode(this.index));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LocalVariableDefEntry && equals((LocalVariableDefEntry) other);
	}

	public boolean equals(LocalVariableDefEntry other) {
		return this.ownerEntry.equals(other.ownerEntry) && this.desc.equals(other.desc) && this.name.equals(other.name) && this.index == other.index;
	}

	@Override
	public String toString() {
		return this.ownerEntry + "(" + this.index + ":" + this.name + ":" + this.desc + ")";
	}
}
