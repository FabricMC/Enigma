package cuchaz.enigma.classhandle;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import cuchaz.enigma.events.ClassHandleListener;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.Result;

/**
 * A handle to a class file. Can be treated similarly to a handle to a file.
 * This type allows for accessing decompiled classes and being notified when
 * mappings for the class it belongs to changes.
 *
 * @see ClassHandleProvider
 */
public interface ClassHandle extends AutoCloseable {
	/**
	 * Gets the reference to this class. This is always obfuscated, for example
	 * {@code net/minecraft/class_1000}.
	 *
	 * @return the obfuscated class reference
	 * @throws IllegalStateException if the class handle is closed
	 */
	ClassEntry getRef();

	/**
	 * Gets the deobfuscated reference to this class, if any.
	 *
	 * @return the deobfuscated reference, or {@code null} if the class is not
	 * mapped
	 * @throws IllegalStateException if the class handle is closed
	 */
	@Nullable
	ClassEntry getDeobfRef();

	/**
	 * Gets the class source, or the error generated while decompiling the
	 * class, asynchronously. If this class has already finished decompiling,
	 * this will return an already completed future.
	 *
	 * @return the class source
	 * @throws IllegalStateException if the class handle is closed
	 */
	CompletableFuture<Result<DecompiledClassSource, ClassHandleError>> getSource();

	/**
	 * Gets the class source without any decoration, or the error generated
	 * while decompiling the class, asynchronously. This is the raw source from
	 * the decompiler and will not be deobfuscated, and does not contain any
	 * Javadoc comments added via mappings. If this class has already finished
	 * decompiling, this will return an already completed future.
	 *
	 * @return the uncommented class source
	 * @throws IllegalStateException if the class handle is closed
	 * @see ClassHandle#getSource()
	 */
	CompletableFuture<Result<Source, ClassHandleError>> getUncommentedSource();

	void invalidate();

	void invalidateMapped();

	void invalidateJavadoc();

	/**
	 * Adds a listener for this class handle.
	 *
	 * @param listener the listener to add
	 * @see ClassHandleListener
	 */
	void addListener(ClassHandleListener listener);

	/**
	 * Removes a previously added listener (with
	 * {@link ClassHandle#addListener(ClassHandleListener)}) from this class
	 * handle.
	 *
	 * @param listener the listener to remove
	 */
	void removeListener(ClassHandleListener listener);

	/**
	 * Copies this class handle. The new class handle points to the same class,
	 * but is independent from this class handle in every other aspect.
	 * Specifically, any listeners will not be copied to the new class handle.
	 *
	 * @return a copy of this class handle
	 * @throws IllegalStateException if the class handle is closed
	 */
	ClassHandle copy();

	/**
	 * {@inheritDoc}
	 *
	 * <p>Specifically, for class handles, this means that most methods on the
	 * handle will throw an exception if called, that the handle will no longer
	 * receive any events over any added listeners, and the handle will no
	 * longer be able to be copied.
	 */
	@Override
	void close();
}
