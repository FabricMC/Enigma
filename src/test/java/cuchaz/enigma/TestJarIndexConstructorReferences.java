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

import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collection;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexConstructorReferences {

	private JarIndex index;

	private ClassEntry baseClass = newClass("a");
	private ClassEntry subClass = newClass("d");
	private ClassEntry subsubClass = newClass("e");
	private ClassEntry defaultClass = newClass("c");
	private ClassEntry callerClass = newClass("b");

	public TestJarIndexConstructorReferences() throws Exception {
		ClassCache classCache = ClassCache.of(Paths.get("build/test-obf/constructors.jar"));
		index = classCache.index(ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(index.getEntryIndex().getClasses(), containsInAnyOrder(newClass("cuchaz/enigma/inputs/Keep"), baseClass,
				subClass, subsubClass, defaultClass, callerClass));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void baseDefault() {
		MethodEntry source = newMethod(baseClass, "<init>", "()V");
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "a", "()V"),
				newBehaviorReferenceByMethod(source, subClass.getName(), "<init>", "()V"),
				newBehaviorReferenceByMethod(source, subClass.getName(), "<init>", "(III)V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void baseInt() {
		MethodEntry source = newMethod(baseClass, "<init>", "(I)V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "b", "()V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subDefault() {
		MethodEntry source = newMethod(subClass, "<init>", "()V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "c", "()V"),
				newBehaviorReferenceByMethod(source, subClass.getName(), "<init>", "(I)V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subInt() {
		MethodEntry source = newMethod(subClass, "<init>", "(I)V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "d", "()V"),
				newBehaviorReferenceByMethod(source, subClass.getName(), "<init>", "(II)V"),
				newBehaviorReferenceByMethod(source, subsubClass.getName(), "<init>", "(I)V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subIntInt() {
		MethodEntry source = newMethod(subClass, "<init>", "(II)V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "e", "()V")
		));
	}

	@Test
	public void subIntIntInt() {
		MethodEntry source = newMethod(subClass, "<init>", "(III)V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), is(empty()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subsubInt() {
		MethodEntry source = newMethod(subsubClass, "<init>", "(I)V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "f", "()V")
		));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultConstructable() {
		MethodEntry source = newMethod(defaultClass, "<init>", "()V");
		assertThat(index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, callerClass.getName(), "g", "()V")
		));
	}
}
