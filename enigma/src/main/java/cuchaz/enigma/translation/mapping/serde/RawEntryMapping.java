package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;

import java.util.ArrayList;
import java.util.List;

public final class RawEntryMapping {
	private final String targetName;
	private final AccessModifier access;
	private List<String> javadocs = new ArrayList<>();

	public RawEntryMapping(String targetName) {
		this(targetName, null);
	}

	public RawEntryMapping(String targetName, AccessModifier access) {
		this.access = access;
		this.targetName = targetName;
	}

	public void addJavadocLine(String line) {
		javadocs.add(line);
	}

	public EntryMapping bake() {
		return new EntryMapping(targetName, access, javadocs.isEmpty() ? null : String.join("\n", javadocs));
	}
}
