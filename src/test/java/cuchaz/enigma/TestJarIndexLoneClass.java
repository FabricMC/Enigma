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
	
	private JarIndex index;
	
	public TestJarIndexLoneClass()
	throws Exception {
		index = new JarIndex();
		index.indexJar(new JarFile("build/test-obf/loneClass.jar"), false);
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
		ClassInheritanceTreeNode node = index.getClassInheritance(new Translator(), newClass("a"));
		assertThat(node, is(not(nullValue())));
		assertThat(node.getObfClassName(), is("a"));
		assertThat(node.getChildCount(), is(0));
	}
	
	@Test
	public void methodInheritance() {
		MethodEntry source = newMethod("a", "a", "()Ljava/lang/String;");
		MethodInheritanceTreeNode node = index.getMethodInheritance(new Translator(), source);
		assertThat(node, is(not(nullValue())));
		assertThat(node.getMethodEntry(), is(source));
		assertThat(node.getChildCount(), is(0));
	}
	
	@Test
	public void classImplementations() {
		ClassImplementationsTreeNode node = index.getClassImplementations(new Translator(), newClass("a"));
		assertThat(node, is(nullValue()));
	}
	
	@Test
	public void methodImplementations() {
		MethodEntry source = newMethod("a", "a", "()Ljava/lang/String;");
		assertThat(index.getMethodImplementations(new Translator(), source), is(empty()));
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
		Collection<EntryReference<FieldEntry,BehaviorEntry>> references = index.getFieldReferences(source);
		assertThat(references, containsInAnyOrder(
			newFieldReferenceByConstructor(source, "a", "(Ljava/lang/String;)V"),
			newFieldReferenceByMethod(source, "a", "a", "()Ljava/lang/String;")
		));
	}
	
	@Test
	public void behaviorReferences() {
		assertThat(index.getBehaviorReferences(newMethod("a", "a", "()Ljava/lang/String;")), is(empty()));
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
	public void isAnonymousClass() {
		assertThat(index.isAnonymousClass(newClass("a")), is(false));
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
		assertThat(index.containsObfBehavior(newMethod("a", "a", "()Ljava/lang/String;")), is(true));
		assertThat(index.containsObfBehavior(newMethod("a", "b", "()Ljava/lang/String;")), is(false));
	}
}
