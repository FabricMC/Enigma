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

import java.util.Collection;
import java.util.Set;
import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.analysis.Access;
import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class TestJarIndexLoneClass {
	
	private JarIndex m_index;
	
	public TestJarIndexLoneClass()
	throws Exception {
		m_index = new JarIndex();
		m_index.indexJar(new JarFile("build/test-obf/loneClass.jar"), false);
	}
	
	@Test
	public void obfEntries() {
		assertThat(m_index.getObfClassEntries(), containsInAnyOrder(
			newClass("cuchaz/enigma/inputs/Keep"),
			newClass("none/a")
		));
	}
	
	@Test
	public void translationIndex() {
		assertThat(m_index.getTranslationIndex().getSuperclass(new ClassEntry("none/a")), is(new ClassEntry("java/lang/Object")));
		assertThat(m_index.getTranslationIndex().getSuperclass(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(new ClassEntry("java/lang/Object")));
		assertThat(m_index.getTranslationIndex().getAncestry(new ClassEntry("none/a")), contains(new ClassEntry("java/lang/Object")));
		assertThat(m_index.getTranslationIndex().getAncestry(new ClassEntry("cuchaz/enigma/inputs/Keep")), contains(new ClassEntry("java/lang/Object")));
		assertThat(m_index.getTranslationIndex().getSubclass(new ClassEntry("none/a")), is(empty()));
		assertThat(m_index.getTranslationIndex().getSubclass(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(empty()));
	}
	
	@Test
	public void access() {
		assertThat(m_index.getAccess(newField("none/a", "a", "Ljava/lang/String;")), is(Access.Private));
		assertThat(m_index.getAccess(newMethod("none/a", "a", "()Ljava/lang/String;")), is(Access.Public));
		assertThat(m_index.getAccess(newField("none/a", "b", "Ljava/lang/String;")), is(nullValue()));
		assertThat(m_index.getAccess(newField("none/a", "a", "LFoo;")), is(nullValue()));
	}
	
	@Test
	public void classInheritance() {
		ClassInheritanceTreeNode node = m_index.getClassInheritance(new Translator(), newClass("none/a"));
		assertThat(node, is(not(nullValue())));
		assertThat(node.getObfClassName(), is("none/a"));
		assertThat(node.getChildCount(), is(0));
	}
	
	@Test
	public void methodInheritance() {
		MethodEntry source = newMethod("none/a", "a", "()Ljava/lang/String;");
		MethodInheritanceTreeNode node = m_index.getMethodInheritance(new Translator(), source);
		assertThat(node, is(not(nullValue())));
		assertThat(node.getMethodEntry(), is(source));
		assertThat(node.getChildCount(), is(0));
	}
	
	@Test
	public void classImplementations() {
		ClassImplementationsTreeNode node = m_index.getClassImplementations(new Translator(), newClass("none/a"));
		assertThat(node, is(nullValue()));
	}
	
	@Test
	public void methodImplementations() {
		MethodEntry source = newMethod("none/a", "a", "()Ljava/lang/String;");
		assertThat(m_index.getMethodImplementations(new Translator(), source), is(empty()));
	}
	
	@Test
	public void relatedMethodImplementations() {
		Set<MethodEntry> entries = m_index.getRelatedMethodImplementations(newMethod("none/a", "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
			newMethod("none/a", "a", "()Ljava/lang/String;")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void fieldReferences() {
		FieldEntry source = newField("none/a", "a", "Ljava/lang/String;");
		Collection<EntryReference<FieldEntry,BehaviorEntry>> references = m_index.getFieldReferences(source);
		assertThat(references, containsInAnyOrder(
			newFieldReferenceByConstructor(source, "none/a", "(Ljava/lang/String;)V"),
			newFieldReferenceByMethod(source, "none/a", "a", "()Ljava/lang/String;")
		));
	}
	
	@Test
	public void behaviorReferences() {
		assertThat(m_index.getBehaviorReferences(newMethod("none/a", "a", "()Ljava/lang/String;")), is(empty()));
	}
	
	@Test
	public void innerClasses() {
		assertThat(m_index.getInnerClasses(newClass("none/a")), is(empty()));
	}
	
	@Test
	public void outerClass() {
		assertThat(m_index.getOuterClass(newClass("a")), is(nullValue()));
	}
	
	@Test
	public void isAnonymousClass() {
		assertThat(m_index.isAnonymousClass(newClass("none/a")), is(false));
	}
	
	@Test
	public void interfaces() {
		assertThat(m_index.getInterfaces("none/a"), is(empty()));
	}
	
	@Test
	public void implementingClasses() {
		assertThat(m_index.getImplementingClasses("none/a"), is(empty()));
	}
	
	@Test
	public void isInterface() {
		assertThat(m_index.isInterface("none/a"), is(false));
	}
	
	@Test
	public void testContains() {
		assertThat(m_index.containsObfClass(newClass("none/a")), is(true));
		assertThat(m_index.containsObfClass(newClass("none/b")), is(false));
		assertThat(m_index.containsObfField(newField("none/a", "a", "Ljava/lang/String;")), is(true));
		assertThat(m_index.containsObfField(newField("none/a", "b", "Ljava/lang/String;")), is(false));
		assertThat(m_index.containsObfField(newField("none/a", "a", "LFoo;")), is(false));
		assertThat(m_index.containsObfBehavior(newMethod("none/a", "a", "()Ljava/lang/String;")), is(true));
		assertThat(m_index.containsObfBehavior(newMethod("none/a", "b", "()Ljava/lang/String;")), is(false));
	}
}
