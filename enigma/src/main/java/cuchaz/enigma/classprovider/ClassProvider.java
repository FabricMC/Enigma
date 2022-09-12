package cuchaz.enigma.classprovider;

import javax.annotation.Nullable;

import org.objectweb.asm.tree.ClassNode;

public interface ClassProvider {
	/**
	 * Gets the {@linkplain ClassNode} for a class. The class provider may return a cached result,
	 * so it's important to not mutate it.
	 *
	 * @param name the internal name of the class
	 * @return the {@linkplain ClassNode} for that class, or {@code null} if it was not found
	 */
	@Nullable
	ClassNode get(String name);
}
