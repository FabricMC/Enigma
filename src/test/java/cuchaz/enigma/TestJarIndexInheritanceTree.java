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

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.IndexEntryResolver;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Collection;
import java.util.jar.JarFile;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexInheritanceTree {

	private JarIndex index;

	private ClassEntry baseClass = newClass("a");
	private ClassEntry subClassA = newClass("b");
	private ClassEntry subClassAA = newClass("d");
	private ClassEntry subClassB = newClass("c");
	private FieldEntry nameField = newField(baseClass, "a", "Ljava/lang/String;");
	private FieldEntry numThingsField = newField(subClassB, "a", "I");

	public TestJarIndexInheritanceTree()
			throws Exception {
		index = JarIndex.empty();
		index.indexJar(new ParsedJar(new JarFile("build/test-obf/inheritanceTree.jar")), s -> {});
	}

	@Test
	public void obfEntries() {
		assertThat(index.getEntryIndex().getClasses(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"), baseClass, subClassA, subClassAA, subClassB
		));
	}

	@Test
	public void translationIndex() {

		InheritanceIndex index = this.index.getInheritanceIndex();

		// base class
		assertThat(index.getParents(baseClass), is(empty()));
		assertThat(index.getAncestors(baseClass), is(empty()));
		assertThat(index.getChildren(baseClass), containsInAnyOrder(subClassA, subClassB
		));

		// subclass a
		assertThat(index.getParents(subClassA), contains(baseClass));
		assertThat(index.getAncestors(subClassA), containsInAnyOrder(baseClass));
		assertThat(index.getChildren(subClassA), contains(subClassAA));

		// subclass aa
		assertThat(index.getParents(subClassAA), contains(subClassA));
		assertThat(index.getAncestors(subClassAA), containsInAnyOrder(subClassA, baseClass));
		assertThat(index.getChildren(subClassAA), is(empty()));

		// subclass b
		assertThat(index.getParents(subClassB), contains(baseClass));
		assertThat(index.getAncestors(subClassB), containsInAnyOrder(baseClass));
		assertThat(index.getChildren(subClassB), is(empty()));
	}

	@Test
	public void access() {
		assertThat(index.getEntryIndex().getFieldAccess(nameField), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
		assertThat(index.getEntryIndex().getFieldAccess(numThingsField), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
	}

	@Test
	public void relatedMethodImplementations() {

		Collection<MethodEntry> entries;

		EntryResolver resolver = new IndexEntryResolver(index);
		// getName()
		entries = resolver.resolveEquivalentMethods(newMethod(baseClass, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()Ljava/lang/String;"),
				newMethod(subClassAA, "a", "()Ljava/lang/String;")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(subClassAA, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()Ljava/lang/String;"),
				newMethod(subClassAA, "a", "()Ljava/lang/String;")
		));

		// doBaseThings()
		entries = resolver.resolveEquivalentMethods(newMethod(baseClass, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()V"),
				newMethod(subClassAA, "a", "()V"),
				newMethod(subClassB, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(subClassAA, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()V"),
				newMethod(subClassAA, "a", "()V"),
				newMethod(subClassB, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(subClassB, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(baseClass, "a", "()V"),
				newMethod(subClassAA, "a", "()V"),
				newMethod(subClassB, "a", "()V")
		));

		// doBThings
		entries = resolver.resolveEquivalentMethods(newMethod(subClassB, "b", "()V"));
		assertThat(entries, containsInAnyOrder(newMethod(subClassB, "b", "()V")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fieldReferences() {
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references;

		// name
		references = index.getReferenceIndex().getReferencesToField(nameField);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(nameField, baseClass.getName(), "<init>", "(Ljava/lang/String;)V"),
				newFieldReferenceByMethod(nameField, baseClass.getName(), "a", "()Ljava/lang/String;")
		));

		// numThings
		references = index.getReferenceIndex().getReferencesToField(numThingsField);
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
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassA.getName(), "<init>", "(Ljava/lang/String;)V"),
				newBehaviorReferenceByMethod(source, subClassB.getName(), "<init>", "()V")
		));

		// subClassA constructor
		source = newMethod(subClassA, "<init>", "(Ljava/lang/String;)V");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassAA.getName(), "<init>", "()V")
		));

		// baseClass.getName()
		source = newMethod(baseClass, "a", "()Ljava/lang/String;");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassAA.getName(), "a", "()Ljava/lang/String;"),
				newBehaviorReferenceByMethod(source, subClassB.getName(), "a", "()V")
		));

		// subclassAA.getName()
		source = newMethod(subClassAA, "a", "()Ljava/lang/String;");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, subClassAA.getName(), "a", "()V")
		));
	}

	@Test
	public void containsEntries() {
		EntryIndex entryIndex = index.getEntryIndex();
		// classes
		assertThat(entryIndex.hasClass(baseClass), is(true));
		assertThat(entryIndex.hasClass(subClassA), is(true));
		assertThat(entryIndex.hasClass(subClassAA), is(true));
		assertThat(entryIndex.hasClass(subClassB), is(true));

		// fields
		assertThat(entryIndex.hasField(nameField), is(true));
		assertThat(entryIndex.hasField(numThingsField), is(true));

		// methods
		// getName()
		assertThat(entryIndex.hasMethod(newMethod(baseClass, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(subClassA, "a", "()Ljava/lang/String;")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(subClassAA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(subClassB, "a", "()Ljava/lang/String;")), is(false));

		// doBaseThings()
		assertThat(entryIndex.hasMethod(newMethod(baseClass, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(subClassA, "a", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(subClassAA, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(subClassB, "a", "()V")), is(true));

		// doBThings()
		assertThat(entryIndex.hasMethod(newMethod(baseClass, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(subClassA, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(subClassAA, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(subClassB, "b", "()V")), is(true));

	}
}
