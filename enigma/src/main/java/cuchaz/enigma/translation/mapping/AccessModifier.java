package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.AccessFlags;

public enum AccessModifier {
	UNCHANGED,
	PUBLIC,
	PROTECTED,
	PRIVATE;

	public String getFormattedName() {
		return "ACC:" + super.toString();
	}

	public AccessFlags transform(AccessFlags access) {
		return switch (this) {
		case PUBLIC -> access.setPublic();
		case PROTECTED -> access.setProtected();
		case PRIVATE -> access.setPrivate();
		default -> access;
		};
	}
}
