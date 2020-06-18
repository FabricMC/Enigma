package cuchaz.enigma.translation.mapping;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EntryMapping {
	public static final EntryMapping DEFAULT = new EntryMapping(null, AccessModifier.UNCHANGED, null);

	private final @Nullable String targetName;
	private final AccessModifier accessModifier;
	private final @Nullable String javadoc;

	public EntryMapping(@Nullable String targetName) {
		this(targetName, AccessModifier.UNCHANGED);
	}

	public EntryMapping(@Nullable String targetName, @Nullable String javadoc) {
		this(targetName, AccessModifier.UNCHANGED, javadoc);
	}

	public EntryMapping(@Nullable String targetName, AccessModifier accessModifier) {
		this(targetName, accessModifier, null);
	}

	public EntryMapping(@Nullable String targetName, AccessModifier accessModifier, @Nullable String javadoc) {
		this.targetName = targetName;
		this.accessModifier = accessModifier;
		this.javadoc = javadoc;
	}

	@Nullable
	public String getTargetName() {
		return this.targetName;
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EntryMapping that = (EntryMapping) o;
		return Objects.equals(targetName, that.targetName) &&
				accessModifier == that.accessModifier &&
				Objects.equals(javadoc, that.javadoc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetName, accessModifier, javadoc);
	}
}
