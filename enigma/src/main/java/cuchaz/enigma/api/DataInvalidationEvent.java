package cuchaz.enigma.api;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

public interface DataInvalidationEvent {
	/**
	 * The classes for which the invalidation applies, or {@code null} if the invalidation applies to all classes.
	 */
	@Nullable
	Collection<String> getClasses();

	InvalidationType getType();

	enum InvalidationType {
		/**
		 * Only mappings are being invalidated.
		 */
		MAPPINGS,
		/**
		 * Javadocs are being invalidated. This also implies {@link #MAPPINGS}.
		 */
		JAVADOC,
		/**
		 * Context passed to the decompiler, such as the bytecode input or other parameters, is being invalidated. This
		 * also implies {@link #JAVADOC} and {@link #MAPPINGS}.
		 */
		DECOMPILE,
	}
}
