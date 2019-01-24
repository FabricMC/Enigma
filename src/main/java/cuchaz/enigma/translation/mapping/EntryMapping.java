package cuchaz.enigma.translation.mapping;

import javax.annotation.Nonnull;

public class EntryMapping {
	private final String targetName;
	private final AccessModifier accessModifier;

	public EntryMapping(@Nonnull String targetName) {
		this(targetName, AccessModifier.UNCHANGED);
	}

	public EntryMapping(@Nonnull String targetName, AccessModifier accessModifier) {
		this.targetName = targetName;
		this.accessModifier = accessModifier;
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
}
