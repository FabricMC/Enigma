package cuchaz.enigma.events;

import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.classhandle.ClassHandleError;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.Result;

public interface ClassHandleListener {
	default void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
	}

	default void onUncommentedSourceChanged(ClassHandle h, Result<Source, ClassHandleError> res) {
	}

	default void onDocsChanged(ClassHandle h, Result<Source, ClassHandleError> res) {
	}

	default void onMappedSourceChanged(ClassHandle h, Result<DecompiledClassSource, ClassHandleError> res) {
	}

	default void onInvalidate(ClassHandle h, InvalidationType t) {
	}

	default void onDeleted(ClassHandle h) {
	}

	enum InvalidationType {
		FULL,
		JAVADOC,
		MAPPINGS,
	}
}
