package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.AccessFlags;

public enum AccessModifier {
	UNCHANGED, PUBLIC, PROTECTED, PRIVATE;

	public String getFormattedName() {
		return "ACC:" + super.toString();
	}

	public AccessFlags transform(AccessFlags access) {
		switch (this) {
			case PUBLIC:
				return access.setPublic();
			case PROTECTED:
				return access.setProtected();
			case PRIVATE:
				return access.setPrivate();
			case UNCHANGED:
			default:
				return access;
		}
	}
}
