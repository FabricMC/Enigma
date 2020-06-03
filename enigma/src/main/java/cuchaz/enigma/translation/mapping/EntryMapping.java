package cuchaz.enigma.translation.mapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntryMapping {
	private final String targetName;
	private final AccessModifier accessModifier;
	private final @Nullable String javadoc;

	public EntryMapping(@Nonnull String targetName) {
		this(targetName, AccessModifier.UNCHANGED);
	}

	public EntryMapping(@Nonnull String targetName, @Nullable String javadoc) {
		this(targetName, AccessModifier.UNCHANGED, javadoc);
	}

	public EntryMapping(@Nonnull String targetName, AccessModifier accessModifier) {
		this(targetName, accessModifier, null);
	}

	public EntryMapping(@Nonnull String targetName, AccessModifier accessModifier, @Nullable String javadoc) {
		this.targetName = targetName;
		this.accessModifier = accessModifier;
		this.javadoc = javadoc;
	}

	@Nonnull
	public String getTargetName() {
		return targetName;
	}

	@Nonnull
	public AccessModifier getAccessModifier() {
		if (accessModifier == null) {
			return AccessModifier.UNCHANGED;
		}
		return accessModifier;
	}

	@Nullable
	public String getJavadoc() {
		return javadoc;
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof EntryMapping) {
			EntryMapping mapping = (EntryMapping) obj;
			return mapping.targetName.equals(targetName) && mapping.accessModifier.equals(accessModifier);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return targetName.hashCode() + accessModifier.hashCode() * 31;
	}
}
