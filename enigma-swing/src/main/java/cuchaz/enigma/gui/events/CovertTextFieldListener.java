package cuchaz.enigma.gui.events;

import cuchaz.enigma.gui.elements.CovertTextField;

public interface CovertTextFieldListener {

	default void onStartEditing(CovertTextField field) {
	}

	default boolean tryStopEditing(CovertTextField field, boolean abort) {
		return true;
	}

	default void onStopEditing(CovertTextField field, boolean abort) {
	}

}
