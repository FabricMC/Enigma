package cuchaz.enigma.translation.representation.entry;

import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;

/**
 * TypeDescriptor...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableEntry extends ParentedEntry<MethodEntry> implements Comparable<LocalVariableEntry> {

	protected final int index;
	protected final boolean parameter;

	public LocalVariableEntry(MethodEntry parent, int index, String name, boolean parameter, String javadoc) {
		super(parent, name, javadoc);

		Preconditions.checkNotNull(parent, "Variable owner cannot be null");
		Preconditions.checkArgument(index >= 0, "Index must be positive");

		this.index = index;
		this.parameter = parameter;
	}

	@Override
	public Class<MethodEntry> getParentType() {
		return MethodEntry.class;
	}

	public boolean isArgument() {
		return this.parameter;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	protected TranslateResult<LocalVariableEntry> extendedTranslate(Translator translator, @Nullable EntryMapping mapping) {
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		String javadoc = mapping != null ? mapping.getJavadoc() : null;
		return TranslateResult.of(
				mapping == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new LocalVariableEntry(parent, index, translatedName, parameter, javadoc)
		);
	}

	@Override
	public LocalVariableEntry withName(String name) {
		return new LocalVariableEntry(parent, index, name, parameter, javadocs);
	}

	@Override
	public LocalVariableEntry withParent(MethodEntry parent) {
		return new LocalVariableEntry(parent, index, name, parameter, javadocs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.index);
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
