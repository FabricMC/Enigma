package cuchaz.enigma.api.view.entry;

import org.jetbrains.annotations.Nullable;

public interface EntryView {
	/**
	 * Returns the default name of this entry.
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 *     <li>Outer class: "domain.name.ClassA"</li>
	 *     <li>Inner class: "ClassB"</li>
	 *     <li>Method: "methodC"</li>
	 * </ul>
	 */
	String getName();

	/**
	 * Returns the full name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's their name prefixed with the full name
	 * of their parent entry.</p>
	 * <p>For other classes, it's their name prefixed with their package name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 *     <li>Outer class: "domain.name.ClassA"</li>
	 *     <li>Inner class: "domain.name.ClassA$ClassB"</li>
	 *     <li>Method: "domain.name.ClassA.methodC"</li>
	 * </ul>
	 */
	String getFullName();

	@Nullable
	String getJavadocs();
}
