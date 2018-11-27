package cuchaz.enigma.mapping.entry;

import com.google.common.base.Preconditions;
import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.utils.Utils;

/**
 * TypeDescriptor...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableEntry implements Entry {

	protected final MethodEntry ownerEntry;
	protected final String name;
	protected final int index;
	protected final boolean parameter;

	@Deprecated
	public LocalVariableEntry(MethodEntry ownerEntry, int index, String name) {
		this(ownerEntry, index, name, true);
	}

	public LocalVariableEntry(MethodEntry ownerEntry, int index, String name, boolean parameter) {
		Preconditions.checkNotNull(ownerEntry, "Variable owner cannot be null");
		Preconditions.checkNotNull(name, "Variable name cannot be null");
		Preconditions.checkArgument(index >= 0, "Index must be positive");

		this.ownerEntry = ownerEntry;
		this.name = name;
		this.index = index;
		this.parameter = parameter;
	}

	public boolean isParameter() {
		return this.parameter;
	}

	public MethodEntry getOwnerEntry() {
		return this.ownerEntry;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public ClassEntry getOwnerClassEntry() {
		return this.ownerEntry.getOwnerClassEntry();
	}

	@Override
	public String getClassName() {
		return this.ownerEntry.getClassName();
	}

	@Override
	public LocalVariableEntry updateOwnership(ClassEntry classEntry) {
		return new LocalVariableEntry(ownerEntry.updateOwnership(classEntry), index, name, parameter);
	}

	public String getMethodName() {
		return this.ownerEntry.getName();
	}

	public MethodDescriptor getMethodDesc() {
		return this.ownerEntry.getDesc();
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.ownerEntry, this.name.hashCode(), Integer.hashCode(this.index));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LocalVariableEntry && equals((LocalVariableEntry) other);
	}

	public boolean equals(LocalVariableEntry other) {
		return this.ownerEntry.equals(other.ownerEntry) && this.name.equals(other.name) && this.index == other.index;
	}

	@Override
	public String toString() {
		return this.ownerEntry + "(" + this.index + ":" + this.name + ")";
	}
}
