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
	
	private JarIndex index;
	
	private ClassEntry objectClass    = newClass("java/lang/Object");
	private ClassEntry baseClass      = newClass("a");
	private ClassEntry subClassA      = newClass("b");
	private ClassEntry subClassAA     = newClass("d");
	private ClassEntry subClassB      = newClass("c");
	private FieldEntry nameField      = newField(baseClass, "a", "Ljava/lang/String;");
	private FieldEntry numThingsField = newField(subClassB, "a", "I");
	
	public TestJarIndexInheritanceTree()
	throws Exception {
		index = new JarIndex();
		index.indexJar(new JarFile("build/test-obf/inheritanceTree.jar"), false);
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
		Collection<EntryReference<FieldEntry,BehaviorEntry>> references;
		
		// name
		references = index.getFieldReferences(nameField);
		assertThat(references, containsInAnyOrder(
			newFieldReferenceByConstructor(nameField, baseClass.getName(), "(Ljava/lang/String;)V"),
			newFieldReferenceByMethod(nameField, baseClass.getName(), "a", "()Ljava/lang/String;")
		));
		
		// numThings
		references = index.getFieldReferences(numThingsField);
		assertThat(references, containsInAnyOrder(
			newFieldReferenceByConstructor(numThingsField, subClassB.getName(), "()V"),
			newFieldReferenceByMethod(numThingsField, subClassB.getName(), "b", "()V")
		));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void behaviorReferences() {
		
		BehaviorEntry source;
		Collection<EntryReference<BehaviorEntry,BehaviorEntry>> references;
		
		// baseClass constructor
		source = newConstructor(baseClass, "(Ljava/lang/String;)V");
		references = index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByConstructor(source, subClassA.getName(), "(Ljava/lang/String;)V"),
			newBehaviorReferenceByConstructor(source, subClassB.getName(), "()V")
		));
		
		// subClassA constructor
		source = newConstructor(subClassA, "(Ljava/lang/String;)V");
		references = index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByConstructor(source, subClassAA.getName(), "()V")
		));
		
		// baseClass.getName()
		source = newMethod(baseClass, "a", "()Ljava/lang/String;");
		references = index.getBehaviorReferences(source);
		assertThat(references, containsInAnyOrder(
			newBehaviorReferenceByMethod(source, subClassAA.getName(), "a", "()Ljava/lang/String;"),
			newBehaviorReferenceByMethod(source, subClassB.getName(), "a", "()V")
		));
		
		// subclassAA.getName()
		source = newMethod(subClassAA, "a", "()Ljava/lang/String;");
		references = index.getBehaviorReferences(source);
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
		assertThat(index.containsObfBehavior(newMethod(baseClass, "a", "()Ljava/lang/String;")), is(true));
		assertThat(index.containsObfBehavior(newMethod(subClassA, "a", "()Ljava/lang/String;")), is(false));
		assertThat(index.containsObfBehavior(newMethod(subClassAA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(index.containsObfBehavior(newMethod(subClassB, "a", "()Ljava/lang/String;")), is(false));
		
		// doBaseThings()
		assertThat(index.containsObfBehavior(newMethod(baseClass, "a", "()V")), is(true));
		assertThat(index.containsObfBehavior(newMethod(subClassA, "a", "()V")), is(false));
		assertThat(index.containsObfBehavior(newMethod(subClassAA, "a", "()V")), is(true));
		assertThat(index.containsObfBehavior(newMethod(subClassB, "a", "()V")), is(true));
		
		// doBThings()
		assertThat(index.containsObfBehavior(newMethod(baseClass, "b", "()V")), is(false));
		assertThat(index.containsObfBehavior(newMethod(subClassA, "b", "()V")), is(false));
		assertThat(index.containsObfBehavior(newMethod(subClassAA, "b", "()V")), is(false));
		assertThat(index.containsObfBehavior(newMethod(subClassB, "b", "()V")), is(true));
		
	}
}
