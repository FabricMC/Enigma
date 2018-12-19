/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.analysis.*;
import cuchaz.enigma.translation.representation.*;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;
import java.util.jar.JarFile;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexInheritanceTree {

	private JarIndex index;

	private ClassEntry objectClass = newClass("java/lang/Object");
	private ClassEntry baseClass = newClass("a");
	private ClassEntry subClassA = newClass("b");
	private ClassEntry subClassAA = newClass("d");
	private ClassEntry subClassB = newClass("c");
	private FieldEntry nameField = newField(baseClass, "a", "Ljava/lang/String;");
	private FieldEntry numThingsField = newField(subClassB, "a", "I");

	public TestJarIndexInheritanceTree()
			throws Exception {
		index = new JarIndex(new ReferencedEntryPool());
		index.indexJar(new ParsedJar(new JarFile("build/test-obf/inheritanceTree.jar")), false);
	}

	@Test
	public void obfEntries() {
		assertThat(index.getObfClassEntries(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"), baseClass, subClassA, subClassAA, subClassB
		));
	}

	@Test
	public void translationIndex() {

		TranslationIndex index = this.index.getTranslationIndex();

		// base class
		assertThat(index.getSuperclass(baseClass), is(objectClass));
		assertThat(index.getAncestry(baseClass), contains(objectClass));
		assertThat(index.getSubclass(baseClass), containsInAnyOrder(subClassA, subClassB
		));

		// subclass a
		assertThat(index.getSuperclass(subClassA), is(baseClass));
		assertThat(index.getAncestry(subClassA), contains(baseClass, objectClass));
		assertThat(index.getSubclass(subClassA), contains(subClassAA));

		// subclass aa
		assertThat(index.getSuperclass(subClassAA), is(subClassA));
		assertThat(index.getAncestry(subClassAA), contains(subClassA, baseClass, objectClass));
		assertThat(index.getSubclass(subClassAA), is(empty()));

		// subclass b
		assertThat(index.getSuperclass(subClassB), is(baseClass));
		assertThat(index.getAncestry(subClassB), contains(baseClass, objectClass));
		assertThat(index.getSubclass(subClassB), is(empty()));
	}

	@Test
	public void access() {
		assertThat(index.getAccess(nameField), is(Access.PRIVATE));
		assertThat(index.getAccess(numThingsField), is(Access.PRIVATE));
	}

	@Test
	public void relatedMethodImplementations() {

		Set<MethodEntry> entries;

		// getName()
		entries = index.getRelatedMethodImplementations(newMethod(baseClass, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()Ljava/lang/String;"),
				newMethod(subClassAA, "a", "()Ljava/lang/String;")
		));
		entries = index.getRelatedMethodImplementations(newMethod(subClassAA, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()Ljava/lang/String;"),
				newMethod(subClassAA, "a", "()Ljava/lang/String;")
		));

		// doBaseThings()
		entries = index.getRelatedMethodImplementations(newMethod(baseClass, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()V"),
				newMethod(subClassAA, "a", "()V"),
				newMethod(subClassB, "a", "()V")
		));
		entries = index.getRelatedMethodImplementations(newMethod(subClassAA, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()V"),
				newMethod(subClassAA, "a", "()V"),
				newMethod(subClassB, "a", "()V")
		));
		entries = index.getRelatedMethodImplementations(newMethod(subClassB, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()V"),
				newMethod(subClassAA, "a", "()V"),
				newMethod(subClassB, "a", "()V")
		));

		// doBThings
		entries = index.getRelatedMethodImplementations(newMethod(subClassB, "b", "()V"));
		assertThat(entries, containsInAnyOrder(newMethod(subClassB, "b", "()V")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fieldReferences() {
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references;

		// name
		references = index.getFieldReferences(nameField);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(nameField, baseClass.getName(), "<init>", "(Ljava/lang/String;)V"),
				newFieldReferenceByMethod(nameField, baseClass.getName(), "a", "()Ljava/lang/String;")
		));

		// numThings
		references = index.getFieldReferences(numThingsField);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(numThingsField, subClassB.getName(), "<init>", "()V"),
				newFieldReferenceByMethod(numThingsField, subClassB.getName(), "b", "()V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void behaviorReferences() {

		MethodEntry source;
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references;

		// baseClass constructor
		source = newMethod(baseClass, "<init>", "(Ljava/lang/String;)V");
		references = index.getMethodsReferencing(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassA.getName(), "<init>", "(Ljava/lang/String;)V"),
				newBehaviorReferenceByMethod(source, subClassB.getName(), "<init>", "()V")
		));

		// subClassA constructor
		source = newMethod(subClassA, "<init>", "(Ljava/lang/String;)V");
		references = index.getMethodsReferencing(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassAA.getName(), "<init>", "()V")
		));

		// baseClass.getName()
		source = newMethod(baseClass, "a", "()Ljava/lang/String;");
		references = index.getMethodsReferencing(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassAA.getName(), "a", "()Ljava/lang/String;"),
				newBehaviorReferenceByMethod(source, subClassB.getName(), "a", "()V")
		));

		// subclassAA.getName()
		source = newMethod(subClassAA, "a", "()Ljava/lang/String;");
		references = index.getMethodsReferencing(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassAA.getName(), "a", "()V")
		));
	}

	@Test
	public void containsEntries() {

		// classes
		assertThat(index.containsObfClass(baseClass), is(true));
		assertThat(index.containsObfClass(subClassA), is(true));
		assertThat(index.containsObfClass(subClassAA), is(true));
		assertThat(index.containsObfClass(subClassB), is(true));

		// fields
		assertThat(index.containsObfField(nameField), is(true));
		assertThat(index.containsObfField(numThingsField), is(true));

		// methods
		// getName()
		assertThat(index.containsObfMethod(newMethod(baseClass, "a", "()Ljava/lang/String;")), is(true));
		assertThat(index.containsObfMethod(newMethod(subClassA, "a", "()Ljava/lang/String;")), is(false));
		assertThat(index.containsObfMethod(newMethod(subClassAA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(index.containsObfMethod(newMethod(subClassB, "a", "()Ljava/lang/String;")), is(false));

		// doBaseThings()
		assertThat(index.containsObfMethod(newMethod(baseClass, "a", "()V")), is(true));
		assertThat(index.containsObfMethod(newMethod(subClassA, "a", "()V")), is(false));
		assertThat(index.containsObfMethod(newMethod(subClassAA, "a", "()V")), is(true));
		assertThat(index.containsObfMethod(newMethod(subClassB, "a", "()V")), is(true));

		// doBThings()
		assertThat(index.containsObfMethod(newMethod(baseClass, "b", "()V")), is(false));
		assertThat(index.containsObfMethod(newMethod(subClassA, "b", "()V")), is(false));
		assertThat(index.containsObfMethod(newMethod(subClassAA, "b", "()V")), is(false));
		assertThat(index.containsObfMethod(newMethod(subClassB, "b", "()V")), is(true));

	}
}
