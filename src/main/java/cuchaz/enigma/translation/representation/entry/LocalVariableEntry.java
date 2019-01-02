package cuchaz.enigma.translation.representation.entry;

import com.google.common.base.Preconditions;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TypeDescriptor...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableEntry extends ParentedEntry<MethodEntry> implements Comparable<LocalVariableEntry> {

	protected final int index;
	protected final boolean parameter;

	@Deprecated
	public LocalVariableEntry(MethodEntry parent, int index, String name) {
		this(parent, index, name, true);
	}

	public LocalVariableEntry(MethodEntry parent, int index, String name, boolean parameter) {
		super(parent, name);

		Preconditions.checkNotNull(parent, "Variable owner cannot be null");
		Preconditions.checkArgument(index >= 0, "Index must be positive");

		this.index = index;
		this.parameter = parameter;
	}

	public boolean isParameter() {
		return this.parameter;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Nonnull
	@Override
	public MethodEntry getParent() {
		return this.parent;
	}

	@Override
	public LocalVariableEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		return new LocalVariableEntry(parent, index, translatedName, parameter);
	}

	@Override
	public LocalVariableEntry withParent(MethodEntry parent) {
		return new LocalVariableEntry(parent, index, name, parameter);
	}

	@Override
	public int hashCode() {
		return Utils.combineHashesOrdered(this.parent, this.index);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LocalVariableEntry && equals((LocalVariableEntry) other);
	}

	public boolean equals(LocalVariableEntry other) {
		return this.parent.equals(other.parent) && this.index == other.index;
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		return entry instanceof LocalVariableEntry && ((LocalVariableEntry) entry).parent.equals(parent);
	}

	@Override
	public String toString() {
		return this.parent + "(" + this.index + ":" + this.name + ")";
	}

	@Override
	public int compareTo(LocalVariableEntry entry) {
		return Integer.compare(index, entry.index);
	}
}
