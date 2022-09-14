package cuchaz.enigma.translation.mapping;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record EntryMapping(@Nullable String targetName, @Nonnull AccessModifier accessModifier, @Nullable String javadoc) {
	public static final EntryMapping DEFAULT = new EntryMapping(null, AccessModifier.UNCHANGED, null);

	public EntryMapping {
		if (accessModifier == null) {
			accessModifier = AccessModifier.UNCHANGED;
			System.err.println("EntryMapping initialized with 'null' accessModifier, assuming UNCHANGED. Please fix.");
			Arrays.stream(new Exception().getStackTrace()).skip(1).map("\tat %s"::formatted).forEach(System.err::println);
		}
	}

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
