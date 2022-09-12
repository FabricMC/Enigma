package cuchaz.enigma.gui.newabstraction;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryValidation {
	public static boolean validateJavadoc(ValidationContext vc, String javadoc) {
		if (javadoc.contains("*/")) {
			vc.raise(Message.ILLEGAL_DOC_COMMENT_END);
			return false;
		}

		return true;
	}

	public static boolean validateRename(ValidationContext vc, EnigmaProject p, Entry<?> entry, String newName) {
		return p.getMapper().getValidator().validateRename(vc, entry, newName);
	}
}
