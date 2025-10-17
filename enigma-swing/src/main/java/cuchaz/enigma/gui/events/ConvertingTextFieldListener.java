package cuchaz.enigma.gui.events;

import cuchaz.enigma.gui.elements.ConvertingTextField;

public interface ConvertingTextFieldListener {
	enum StopEditingCause {
		ABORT,
		DO,
		TAB
	}

	default void onStartEditing(ConvertingTextField field) {
	}

	default boolean tryStopEditing(ConvertingTextField field, StopEditingCause cause) {
		return true;
	}

	default void onStopEditing(ConvertingTextField field, StopEditingCause cause) {
	}
}
