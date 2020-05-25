package cuchaz.enigma.gui.events;

import cuchaz.enigma.gui.elements.ConvertingTextField;

public interface ConvertingTextFieldListener {

	default void onStartEditing(ConvertingTextField field) {
	}

	default boolean tryStopEditing(ConvertingTextField field, boolean abort) {
		return true;
	}

	default void onStopEditing(ConvertingTextField field, boolean abort) {
	}

}
