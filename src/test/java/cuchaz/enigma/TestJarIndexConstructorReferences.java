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
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.jar.JarFile;

import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByConstructor;
import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByMethod;
import static cuchaz.enigma.TestEntryFactory.newClass;
import static cuchaz.enigma.TestEntryFactory.newConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class TestJarIndexConstructorReferences {

	private JarIndex index;

	private ClassEntry baseClass = newClass("a");
	private ClassEntry subClass = newClass("d");
	private ClassEntry subsubClass = newClass("e");
	private ClassEntry defaultClass = newClass("c");
	private ClassEntry callerClass = newClass("b");

	public TestJarIndexConstructorReferences()
		throws Exception {
		File jarFile = new File("build/test-obf/constructors.jar");
		index = new JarIndex();
		index.indexJar(new JarFile(jarFile), false);
	}

	@Test
	public void obfEntries() {
		assertThat(index.getObfClassEntries(), containsInAnyOrder(newClass("cuchaz/enigma/inputs/Keep"), baseClass,
			subClass, subsubClass, defaultClass, callerClass));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void baseDefault() {
		BehaviorEntry source = newConstructor(baseClass, "()V");
		Collection<EntryReference<BehaviorEntry, BehaviorEntry>> references = index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "a", "()V"),
			newBehaviorReferenceByConstructor(source, subClass.getName(), "()V"),
			newBehaviorReferenceByConstructor(source, subClass.getName(), "(III)V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void baseInt() {
		BehaviorEntry source = newConstructor(baseClass, "(I)V");
		assertThat(index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "b", "()V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subDefault() {
		BehaviorEntry source = newConstructor(subClass, "()V");
		assertThat(index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "c", "()V"),
			newBehaviorReferenceByConstructor(source, subClass.getName(), "(I)V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subInt() {
		BehaviorEntry source = newConstructor(subClass, "(I)V");
		assertThat(index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "d", "()V"),
			newBehaviorReferenceByConstructor(source, subClass.getName(), "(II)V"),
			newBehaviorReferenceByConstructor(source, subsubClass.getName(), "(I)V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subIntInt() {
		BehaviorEntry source = newConstructor(subClass, "(II)V");
		assertThat(index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "e", "()V")
		));
	}

	@Test
	public void subIntIntInt() {
		BehaviorEntry source = newConstructor(subClass, "(III)V");
		assertThat(index.getBehaviorReferences(source), is(empty()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subsubInt() {
		BehaviorEntry source = newConstructor(subsubClass, "(I)V");
		assertThat(index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "f", "()V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultConstructable() {
		BehaviorEntry source = newConstructor(defaultClass, "()V");
		assertThat(index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, callerClass.getName(), "g", "()V")
		));
	}
}
