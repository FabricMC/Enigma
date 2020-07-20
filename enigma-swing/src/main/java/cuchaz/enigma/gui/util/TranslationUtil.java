package cuchaz.enigma.gui.util;

import java.util.ArrayList;
import java.util.List;

public final class TranslationUtil {

	private static final List<TranslationChangeListener> listeners = new ArrayList<>();

	public TranslationUtil() {
	}

	public static void addListener(TranslationChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(TranslationChangeListener listener) {
		listeners.remove(listener);
	}

	public static void dispatchLanguageChange() {
		listeners.forEach(TranslationChangeListener::retranslateUi);
	}

}
