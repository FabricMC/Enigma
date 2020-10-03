package cuchaz.enigma.translation.mapping.serde;

import java.util.ArrayList;
import java.util.List;

import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;

public final class RawEntryMapping {
	private final String targetName;
	private final AccessModifier access;
	private final List<String> javadocs = new ArrayList<>();

	public RawEntryMapping(String targetName) {
		this(targetName, null);
	}

	public RawEntryMapping(String targetName, AccessModifier access) {
		this.access = access;
		this.targetName = targetName != null && !targetName.equals("-") ? targetName : null;
	}

	public void addJavadocLine(String line) {
		javadocs.add(line);
	}

	public EntryMapping bake() {
		return new EntryMapping(targetName, access, javadocs.isEmpty() ? null : String.join("\n", javadocs));
	}
}
