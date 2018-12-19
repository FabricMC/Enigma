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
import cuchaz.enigma.translation.representation.ReferencedEntryPool;
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

public class TestJarIndexLoneClass {

	private JarIndex index;

	public TestJarIndexLoneClass()
			throws Exception {
		index = new JarIndex(new ReferencedEntryPool());
		index.indexJar(new ParsedJar(new JarFile("build/test-obf/loneClass.jar")), false);
	}

	@Test
	public void obfEntries() {
		assertThat(index.getObfClassEntries(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"),
				newClass("a")
		));
	}

	@Test
	public void translationIndex() {
		assertThat(index.getTranslationIndex().getSuperclass(new ClassEntry("a")), is(new ClassEntry("java/lang/Object")));
		assertThat(index.getTranslationIndex().getSuperclass(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(new ClassEntry("java/lang/Object")));
		assertThat(index.getTranslationIndex().getAncestry(new ClassEntry("a")), contains(new ClassEntry("java/lang/Object")));
		assertThat(index.getTranslationIndex().getAncestry(new ClassEntry("cuchaz/enigma/inputs/Keep")), contains(new ClassEntry("java/lang/Object")));
		assertThat(index.getTranslationIndex().getSubclass(new ClassEntry("a")), is(empty()));
		assertThat(index.getTranslationIndex().getSubclass(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(empty()));
	}

	@Test
	public void access() {
		assertThat(index.getAccess(newField("a", "a", "Ljava/lang/String;")), is(Access.PRIVATE));
		assertThat(index.getAccess(newMethod("a", "a", "()Ljava/lang/String;")), is(Access.PUBLIC));
		assertThat(index.getAccess(newField("a", "b", "Ljava/lang/String;")), is(nullValue()));
		assertThat(index.getAccess(newField("a", "a", "LFoo;")), is(nullValue()));
	}

	@Test
	public void classInheritance() {
		ClassInheritanceTreeNode node = index.getClassInheritance(newClass("a"));
		assertThat(node, is(not(nullValue())));
		assertThat(node.getObfClassName(), is("a"));
		assertThat(node.getChildCount(), is(0));
	}

	@Test
	public void methodInheritance() {
		MethodEntry source = newMethod("a", "a", "()Ljava/lang/String;");
		MethodInheritanceTreeNode node = index.getMethodInheritance(source);
		assertThat(node, is(not(nullValue())));
		assertThat(node.getMethodEntry(), is(source));
		assertThat(node.getChildCount(), is(0));
	}

	@Test
	public void classImplementations() {
		ClassImplementationsTreeNode node = index.getClassImplementations(newClass("a"));
		assertThat(node, is(nullValue()));
	}

	@Test
	public void methodImplementations() {
		MethodEntry source = newMethod("a", "a", "()Ljava/lang/String;");
		assertThat(index.getMethodImplementations(source), is(empty()));
	}

	@Test
	public void relatedMethodImplementations() {
		Set<MethodEntry> entries = index.getRelatedMethodImplementations(newMethod("a", "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod("a", "a", "()Ljava/lang/String;")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fieldReferences() {
		FieldEntry source = newField("a", "a", "Ljava/lang/String;");
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references = index.getFieldReferences(source);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(source, "a", "<init>", "(Ljava/lang/String;)V"),
				newFieldReferenceByMethod(source, "a", "a", "()Ljava/lang/String;")
		));
	}

	@Test
	public void behaviorReferences() {
		assertThat(index.getMethodsReferencing(newMethod("a", "a", "()Ljava/lang/String;")), is(empty()));
	}

	@Test
	public void innerClasses() {
		assertThat(index.getInnerClasses(newClass("a")), is(empty()));
	}

	@Test
	public void outerClass() {
		assertThat(index.getOuterClass(newClass("a")), is(nullValue()));
	}

	@Test
	public void interfaces() {
		assertThat(index.getInterfaces("a"), is(empty()));
	}

	@Test
	public void implementingClasses() {
		assertThat(index.getImplementingClasses("a"), is(empty()));
	}

	@Test
	public void isInterface() {
		assertThat(index.isInterface("a"), is(false));
	}

	@Test
	public void testContains() {
		assertThat(index.containsObfClass(newClass("a")), is(true));
		assertThat(index.containsObfClass(newClass("b")), is(false));
		assertThat(index.containsObfField(newField("a", "a", "Ljava/lang/String;")), is(true));
		assertThat(index.containsObfField(newField("a", "b", "Ljava/lang/String;")), is(false));
		assertThat(index.containsObfField(newField("a", "a", "LFoo;")), is(false));
		assertThat(index.containsObfMethod(newMethod("a", "a", "()Ljava/lang/String;")), is(true));
		assertThat(index.containsObfMethod(newMethod("a", "b", "()Ljava/lang/String;")), is(false));
	}
}
