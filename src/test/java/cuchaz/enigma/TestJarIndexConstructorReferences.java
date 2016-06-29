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

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.Collection;
import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;

public class TestJarIndexConstructorReferences {
	
	private JarIndex m_index;
	
	private ClassEntry m_baseClass = newClass("none/a");
	private ClassEntry m_subClass = newClass("none/d");
	private ClassEntry m_subsubClass = newClass("none/e");
	private ClassEntry m_defaultClass = newClass("none/c");
	private ClassEntry m_callerClass = newClass("none/b");
	
	public TestJarIndexConstructorReferences()
	throws Exception {
		File jarFile = new File("build/test-obf/constructors.jar");
		m_index = new JarIndex();
		m_index.indexJar(new JarFile(jarFile), false);
	}
	
	@Test
	public void obfEntries() {
		assertThat(m_index.getObfClassEntries(), containsInAnyOrder(newClass("cuchaz/enigma/inputs/Keep"), m_baseClass, m_subClass, m_subsubClass, m_defaultClass, m_callerClass));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void baseDefault() {
		BehaviorEntry source = newConstructor(m_baseClass, "()V");
		Collection<EntryReference<BehaviorEntry,BehaviorEntry>> references = m_index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "a", "()V"),
			newBehaviorReferenceByConstructor(source, m_subClass.getName(), "()V"),
			newBehaviorReferenceByConstructor(source, m_subClass.getName(), "(III)V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void baseInt() {
		BehaviorEntry source = newConstructor(m_baseClass, "(I)V");
		assertThat(m_index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "b", "()V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void subDefault() {
		BehaviorEntry source = newConstructor(m_subClass, "()V");
		assertThat(m_index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "c", "()V"),
			newBehaviorReferenceByConstructor(source, m_subClass.getName(), "(I)V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void subInt() {
		BehaviorEntry source = newConstructor(m_subClass, "(I)V");
		assertThat(m_index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "d", "()V"),
			newBehaviorReferenceByConstructor(source, m_subClass.getName(), "(II)V"),
			newBehaviorReferenceByConstructor(source, m_subsubClass.getName(), "(I)V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void subIntInt() {
		BehaviorEntry source = newConstructor(m_subClass, "(II)V");
		assertThat(m_index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "e", "()V")
		));
	}
	
	@Test
	public void subIntIntInt() {
		BehaviorEntry source = newConstructor(m_subClass, "(III)V");
		assertThat(m_index.getBehaviorReferences(source), is(empty()));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void subsubInt() {
		BehaviorEntry source = newConstructor(m_subsubClass, "(I)V");
		assertThat(m_index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "f", "()V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void defaultConstructable() {
		BehaviorEntry source = newConstructor(m_defaultClass, "()V");
		assertThat(m_index.getBehaviorReferences(source), containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_callerClass.getName(), "g", "()V")
		));
	}
}
