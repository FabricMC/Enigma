package cuchaz.enigma.gui.util;

import java.util.ArrayList;
import java.util.List;

public final class LanguageUtil {
	private static final List<LanguageChangeListener> listeners = new ArrayList<>();

	public LanguageUtil() {
	}

	public static void addListener(LanguageChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(LanguageChangeListener listener) {
		listeners.remove(listener);
	}

	public static void dispatchLanguageChange() {
		listeners.forEach(LanguageChangeListener::retranslateUi);
	}
}
