package cuchaz.enigma.gui.events;

import cuchaz.enigma.gui.DecompiledClassSource;
import cuchaz.enigma.gui.util.ClassHandle;
import cuchaz.enigma.source.Source;

public interface ClassHandleListener {

	default void onDeobfRefChanged(ClassHandle h) {
	}

	default void onUncommentedSourceChanged(ClassHandle h, Source s) {
	}

	default void onDocsChanged(ClassHandle h, Source s) {
	}

	default void onMappedSourceChanged(ClassHandle h, DecompiledClassSource s) {
	}

	default void onDeleted(ClassHandle h) {
	}

}