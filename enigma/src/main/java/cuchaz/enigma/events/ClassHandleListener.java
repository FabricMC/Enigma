package cuchaz.enigma.events;

import cuchaz.enigma.gui.DecompiledClassSource;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public interface ClassHandleListener {

	default void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
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
