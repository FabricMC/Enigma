package cuchaz.enigma.gui.util;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import cuchaz.enigma.gui.DecompiledClassSource;
import cuchaz.enigma.gui.events.ClassHandleListener;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public interface ClassHandle extends AutoCloseable {

	ClassEntry getRef();

	@Nullable
	ClassEntry getDeobfRef();

	CompletableFuture<DecompiledClassSource> getSource();

	CompletableFuture<Source> getUncommentedSource();

	void addListener(ClassHandleListener listener);

	void removeListener(ClassHandleListener listener);

	ClassHandle copy();

	@Override
	void close();

}
