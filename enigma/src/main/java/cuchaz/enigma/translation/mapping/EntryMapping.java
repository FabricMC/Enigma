package cuchaz.enigma.translation.mapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record EntryMapping(
		@Nullable String targetName,
		@Nonnull AccessModifier accessModifier,
		@Nullable String javadoc
) {
	public static final EntryMapping DEFAULT = new EntryMapping(null, AccessModifier.UNCHANGED, null);

	public EntryMapping(@Nullable String targetName) {
		this(targetName, AccessModifier.UNCHANGED);
	}

	public EntryMapping(@Nullable String targetName, @Nullable String javadoc) {
		this(targetName, AccessModifier.UNCHANGED, javadoc);
	}

	public EntryMapping(@Nullable String targetName, AccessModifier accessModifier) {
		this(targetName, accessModifier, null);
	}

	public EntryMapping withName(String newName) {
		return new EntryMapping(newName, accessModifier, javadoc);
	}

	public EntryMapping withModifier(AccessModifier newModifier) {
		return new EntryMapping(targetName, newModifier, javadoc);
	}

	public EntryMapping withDocs(String newDocs) {
		return new EntryMapping(targetName, accessModifier, newDocs);
	}
}
