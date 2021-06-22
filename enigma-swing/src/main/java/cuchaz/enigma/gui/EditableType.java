package cuchaz.enigma.gui;

import javax.annotation.Nullable;

import cuchaz.enigma.translation.representation.entry.*;

public enum EditableType {
	CLASS,
	METHOD,
	FIELD,
	PARAMETER,
	LOCAL_VARIABLE,
	JAVADOC,
	;

	@Nullable
	public static EditableType fromEntry(Entry<?> entry) {
		// TODO get rid of this with Entry rework
		EditableType type = null;

		if (entry instanceof ClassEntry) {
			type = EditableType.CLASS;
		} else if (entry instanceof MethodEntry me) {
			if (me.isConstructor()) {
				// treat constructors as classes because renaming one renames
				// the class
				type = EditableType.CLASS;
			} else {
				type = EditableType.METHOD;
			}
		} else if (entry instanceof FieldEntry) {
			type = EditableType.FIELD;
		} else if (entry instanceof LocalVariableEntry lve) {
			if (lve.isArgument()) {
				type = EditableType.PARAMETER;
			} else {
				type = EditableType.LOCAL_VARIABLE;
			}
		}

		return type;
	}
}
