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

import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByConstructor;
import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByMethod;
import static cuchaz.enigma.TestEntryFactory.newClass;
import static cuchaz.enigma.TestEntryFactory.newConstructor;
import static cuchaz.enigma.TestEntryFactory.newField;
import static cuchaz.enigma.TestEntryFactory.newFieldReferenceByConstructor;
import static cuchaz.enigma.TestEntryFactory.newFieldReferenceByMethod;
import static cuchaz.enigma.TestEntryFactory.newMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Collection;
import java.util.Set;
import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.analysis.Access;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.analysis.TranslationIndex;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class TestJarIndexInheritanceTree {
	
	private JarIndex m_index;
	
	private ClassEntry m_objectClass = newClass("java/lang/Object");
	private ClassEntry m_baseClass = newClass("none/a");
	private ClassEntry m_subClassA = newClass("none/b");
	private ClassEntry m_subClassAA = newClass("none/d");
	private ClassEntry m_subClassB = newClass("none/c");
	private FieldEntry m_nameField = newField(m_baseClass, "a", "Ljava/lang/String;");
	private FieldEntry m_numThingsField = newField(m_subClassB, "a", "I");
	
	public TestJarIndexInheritanceTree()
	throws Exception {
		m_index = new JarIndex();
		m_index.indexJar(new JarFile("build/test-obf/inheritanceTree.jar"), false);
	}
	
	@Test
	public void obfEntries() {
		assertThat(m_index.getObfClassEntries(), containsInAnyOrder(
			newClass("cuchaz/enigma/inputs/Keep"),
			m_baseClass,
			m_subClassA,
			m_subClassAA,
			m_subClassB
		));
	}
	
	@Test
	public void translationIndex() {
		
		TranslationIndex index = m_index.getTranslationIndex();
		
		// base class
		assertThat(index.getSuperclass(m_baseClass), is(m_objectClass));
		assertThat(index.getAncestry(m_baseClass), contains(m_objectClass));
		assertThat(index.getSubclass(m_baseClass), containsInAnyOrder(
			m_subClassA,
			m_subClassB
		));
		
		// subclass a
		assertThat(index.getSuperclass(m_subClassA), is(m_baseClass));
		assertThat(index.getAncestry(m_subClassA), contains(m_baseClass, m_objectClass));
		assertThat(index.getSubclass(m_subClassA), contains(m_subClassAA));
		
		// subclass aa
		assertThat(index.getSuperclass(m_subClassAA), is(m_subClassA));
		assertThat(index.getAncestry(m_subClassAA), contains(m_subClassA, m_baseClass, m_objectClass));
		assertThat(index.getSubclass(m_subClassAA), is(empty()));
		
		// subclass b
		assertThat(index.getSuperclass(m_subClassB), is(m_baseClass));
		assertThat(index.getAncestry(m_subClassB), contains(m_baseClass, m_objectClass));
		assertThat(index.getSubclass(m_subClassB), is(empty()));
	}
	
	@Test
	public void access() {
		assertThat(m_index.getAccess(m_nameField), is(Access.Private));
		assertThat(m_index.getAccess(m_numThingsField), is(Access.Private));
	}
	
	@Test
	public void relatedMethodImplementations() {
		
		Set<MethodEntry> entries;
		
		// getName()
		entries = m_index.getRelatedMethodImplementations(newMethod(m_baseClass, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
			newMethod(m_baseClass, "a", "()Ljava/lang/String;"),
			newMethod(m_subClassAA, "a", "()Ljava/lang/String;")
		));
		entries = m_index.getRelatedMethodImplementations(newMethod(m_subClassAA, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
			newMethod(m_baseClass, "a", "()Ljava/lang/String;"),
			newMethod(m_subClassAA, "a", "()Ljava/lang/String;")
		));
		
		// doBaseThings()
		entries = m_index.getRelatedMethodImplementations(newMethod(m_baseClass, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
			newMethod(m_baseClass, "a", "()V"),
			newMethod(m_subClassAA, "a", "()V"),
			newMethod(m_subClassB, "a", "()V")
		));
		entries = m_index.getRelatedMethodImplementations(newMethod(m_subClassAA, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
			newMethod(m_baseClass, "a", "()V"),
			newMethod(m_subClassAA, "a", "()V"),
			newMethod(m_subClassB, "a", "()V")
		));
		entries = m_index.getRelatedMethodImplementations(newMethod(m_subClassB, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
			newMethod(m_baseClass, "a", "()V"),
			newMethod(m_subClassAA, "a", "()V"),
			newMethod(m_subClassB, "a", "()V")
		));
		
		// doBThings
		entries = m_index.getRelatedMethodImplementations(newMethod(m_subClassB, "b", "()V"));
		assertThat(entries, containsInAnyOrder(newMethod(m_subClassB, "b", "()V")));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void fieldReferences() {
		Collection<EntryReference<FieldEntry,BehaviorEntry>> references;
		
		// name
		references = m_index.getFieldReferences(m_nameField);
		assertThat(references, containsInAnyOrder(
			newFieldReferenceByConstructor(m_nameField, m_baseClass.getName(), "(Ljava/lang/String;)V"),
			newFieldReferenceByMethod(m_nameField, m_baseClass.getName(), "a", "()Ljava/lang/String;")
		));
		
		// numThings
		references = m_index.getFieldReferences(m_numThingsField);
		assertThat(references, containsInAnyOrder(
			newFieldReferenceByConstructor(m_numThingsField, m_subClassB.getName(), "()V"),
			newFieldReferenceByMethod(m_numThingsField, m_subClassB.getName(), "b", "()V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void behaviorReferences() {
		
		BehaviorEntry source;
		Collection<EntryReference<BehaviorEntry,BehaviorEntry>> references;
		
		// baseClass constructor
		source = newConstructor(m_baseClass, "(Ljava/lang/String;)V");
		references = m_index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByConstructor(source, m_subClassA.getName(), "(Ljava/lang/String;)V"),
			newBehaviorReferenceByConstructor(source, m_subClassB.getName(), "()V")
		));
		
		// subClassA constructor
		source = newConstructor(m_subClassA, "(Ljava/lang/String;)V");
		references = m_index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByConstructor(source, m_subClassAA.getName(), "()V")
		));
		
		// baseClass.getName()
		source = newMethod(m_baseClass, "a", "()Ljava/lang/String;");
		references = m_index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_subClassAA.getName(), "a", "()Ljava/lang/String;"),
			newBehaviorReferenceByMethod(source, m_subClassB.getName(), "a", "()V")
		));
		
		// subclassAA.getName()
		source = newMethod(m_subClassAA, "a", "()Ljava/lang/String;");
		references = m_index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByMethod(source, m_subClassAA.getName(), "a", "()V")
		));
	}
	
	@Test
	public void containsEntries() {
		
		// classes
		assertThat(m_index.containsObfClass(m_baseClass), is(true));
		assertThat(m_index.containsObfClass(m_subClassA), is(true));
		assertThat(m_index.containsObfClass(m_subClassAA), is(true));
		assertThat(m_index.containsObfClass(m_subClassB), is(true));
		
		// fields
		assertThat(m_index.containsObfField(m_nameField), is(true));
		assertThat(m_index.containsObfField(m_numThingsField), is(true));
		
		// methods
		// getName()
		assertThat(m_index.containsObfBehavior(newMethod(m_baseClass, "a", "()Ljava/lang/String;")), is(true));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassA, "a", "()Ljava/lang/String;")), is(false));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassAA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassB, "a", "()Ljava/lang/String;")), is(false));
		
		// doBaseThings()
		assertThat(m_index.containsObfBehavior(newMethod(m_baseClass, "a", "()V")), is(true));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassA, "a", "()V")), is(false));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassAA, "a", "()V")), is(true));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassB, "a", "()V")), is(true));
		
		// doBThings()
		assertThat(m_index.containsObfBehavior(newMethod(m_baseClass, "b", "()V")), is(false));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassA, "b", "()V")), is(false));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassAA, "b", "()V")), is(false));
		assertThat(m_index.containsObfBehavior(newMethod(m_subClassB, "b", "()V")), is(true));
		
	}
}
