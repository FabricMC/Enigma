/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma;

import static cuchaz.enigma.EntryFactory.newBehaviorReferenceByConstructor;
import static cuchaz.enigma.EntryFactory.newBehaviorReferenceByMethod;
import static cuchaz.enigma.EntryFactory.newClass;
import static cuchaz.enigma.EntryFactory.newFieldReferenceByConstructor;
import static cuchaz.enigma.EntryFactory.newFieldReferenceByMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class TestJarIndexInheritanceTree
{
	private JarIndex m_index;
	
	private ClassEntry m_baseClass = new ClassEntry( "none/a" );
	private ClassEntry m_subClassA = new ClassEntry( "none/b" );
	private ClassEntry m_subClassAA = new ClassEntry( "none/d" );
	private ClassEntry m_subClassB = new ClassEntry( "none/c" );
	private FieldEntry m_nameField = new FieldEntry( m_baseClass, "a" );
	private FieldEntry m_numThingsField = new FieldEntry( m_subClassB, "a" );
	
	public TestJarIndexInheritanceTree( )
	throws Exception
	{
		m_index = new JarIndex();
		m_index.indexJar( new JarFile( "build/libs/testInheritanceTree.obf.jar" ), false );
	}
	
	@Test
	public void obfEntries( )
	{
		assertThat( m_index.getObfClassEntries(), containsInAnyOrder(
			newClass( "cuchaz/enigma/inputs/Keep" ),
			m_baseClass,
			m_subClassA,
			m_subClassAA,
			m_subClassB
		) );
	}
	
	@Test
	public void translationIndex( )
	{
		TranslationIndex index = m_index.getTranslationIndex();
		
		// base class
		assertThat( index.getSuperclassName( m_baseClass.getName() ), is( nullValue() ) );
		assertThat( index.getAncestry( m_baseClass.getName() ), is( empty() ) );
		assertThat( index.getSubclassNames( m_baseClass.getName() ), containsInAnyOrder(
			m_subClassA.getName(),
			m_subClassB.getName()
		) );
		
		// subclass a
		assertThat( index.getSuperclassName( m_subClassA.getName() ), is( m_baseClass.getName() ) );
		assertThat( index.getAncestry( m_subClassA.getName() ), contains( m_baseClass.getName() ) );
		assertThat( index.getSubclassNames( m_subClassA.getName() ), contains( m_subClassAA.getName() ) );
		
		// subclass aa
		assertThat( index.getSuperclassName( m_subClassAA.getName() ), is( m_subClassA.getName() ) );
		assertThat( index.getAncestry( m_subClassAA.getName() ), contains(
			m_subClassA.getName(),
			m_baseClass.getName()
		) );
		assertThat( index.getSubclassNames( m_subClassAA.getName() ), is( empty() ) );
		
		// subclass b
		assertThat( index.getSuperclassName( m_subClassB.getName() ), is( m_baseClass.getName() ) );
		assertThat( index.getAncestry( m_subClassB.getName() ), contains( m_baseClass.getName() ) );
		assertThat( index.getSubclassNames( m_subClassB.getName() ), is( empty() ) );
	}
	
	@Test
	public void access( )
	{
		assertThat( m_index.getAccess( m_nameField ), is( Access.Private ) );
		assertThat( m_index.getAccess( m_numThingsField ), is( Access.Private ) );
	}
	
	@Test
	public void isImplemented( )
	{
		// getName()
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_baseClass, "a", "()Ljava/lang/String;" ) ), is( true ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassA, "a", "()Ljava/lang/String;" ) ), is( false ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassAA, "a", "()Ljava/lang/String;" ) ), is( true ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassB, "a", "()Ljava/lang/String;" ) ), is( false ) );
		
		// doBaseThings()
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_baseClass, "a", "()V" ) ), is( true ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassA, "a", "()V" ) ), is( false ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassAA, "a", "()V" ) ), is( true ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassB, "a", "()V" ) ), is( true ) );
		
		// doBThings()
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_baseClass, "b", "()V" ) ), is( false ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassA, "b", "()V" ) ), is( false ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassAA, "b", "()V" ) ), is( false ) );
		assertThat( m_index.isMethodImplemented( new MethodEntry( m_subClassB, "b", "()V" ) ), is( true ) );
	}

	@Test
	public void relatedMethodImplementations( )
	{
		Set<MethodEntry> entries;
		
		// getName()
		entries = m_index.getRelatedMethodImplementations( new MethodEntry( m_baseClass, "a", "()Ljava/lang/String;" ) );
		assertThat( entries, containsInAnyOrder(
			new MethodEntry( m_baseClass, "a", "()Ljava/lang/String;" ),
			new MethodEntry( m_subClassAA, "a", "()Ljava/lang/String;" )
		) );
		entries = m_index.getRelatedMethodImplementations( new MethodEntry( m_subClassAA, "a", "()Ljava/lang/String;" ) );
		assertThat( entries, containsInAnyOrder(
			new MethodEntry( m_baseClass, "a", "()Ljava/lang/String;" ),
			new MethodEntry( m_subClassAA, "a", "()Ljava/lang/String;" )
		) );
		
		// doBaseThings()
		entries = m_index.getRelatedMethodImplementations( new MethodEntry( m_baseClass, "a", "()V" ) );
		assertThat( entries, containsInAnyOrder(
			new MethodEntry( m_baseClass, "a", "()V" ),
			new MethodEntry( m_subClassAA, "a", "()V" ),
			new MethodEntry( m_subClassB, "a", "()V" )
		) );
		entries = m_index.getRelatedMethodImplementations( new MethodEntry( m_subClassAA, "a", "()V" ) );
		assertThat( entries, containsInAnyOrder(
			new MethodEntry( m_baseClass, "a", "()V" ),
			new MethodEntry( m_subClassAA, "a", "()V" ),
			new MethodEntry( m_subClassB, "a", "()V" )
		) );
		entries = m_index.getRelatedMethodImplementations( new MethodEntry( m_subClassB, "a", "()V" ) );
		assertThat( entries, containsInAnyOrder(
			new MethodEntry( m_baseClass, "a", "()V" ),
			new MethodEntry( m_subClassAA, "a", "()V" ),
			new MethodEntry( m_subClassB, "a", "()V" )
		) );
		
		// doBThings
		entries = m_index.getRelatedMethodImplementations( new MethodEntry( m_subClassB, "b", "()V" ) );
		assertThat( entries, containsInAnyOrder(
			new MethodEntry( m_subClassB, "b", "()V" )
		) );
	}
	
	@Test
	@SuppressWarnings( "unchecked" )
	public void fieldReferences( )
	{
		Collection<EntryReference<FieldEntry,BehaviorEntry>> references;
		
		// name
		references = m_index.getFieldReferences( m_nameField );
		assertThat( references, containsInAnyOrder(
			newFieldReferenceByConstructor( m_nameField, m_baseClass.getName(), "(Ljava/lang/String;)V" ),
			newFieldReferenceByMethod( m_nameField, m_baseClass.getName(), "a", "()Ljava/lang/String;" )
		) );
		
		// numThings
		references = m_index.getFieldReferences( m_numThingsField );
		assertThat( references, containsInAnyOrder(
			newFieldReferenceByConstructor( m_numThingsField, m_subClassB.getName(), "()V" ),
			newFieldReferenceByMethod( m_numThingsField, m_subClassB.getName(), "b", "()V" )
		) );
	}
	
	@Test
	@SuppressWarnings( "unchecked" )
	public void behaviorReferences( )
	{
		BehaviorEntry source;
		Collection<EntryReference<BehaviorEntry,BehaviorEntry>> references;
		
		// baseClass constructor
		source = new ConstructorEntry( m_baseClass, "(Ljava/lang/String;)V" );
		references = m_index.getBehaviorReferences( source );
		assertThat( references, containsInAnyOrder(
			newBehaviorReferenceByConstructor( source, m_subClassA.getName(), "(Ljava/lang/String;)V" ),
			newBehaviorReferenceByConstructor( source, m_subClassB.getName(), "()V" )
		) );
		
		// subClassA constructor
		source = new ConstructorEntry( m_subClassA, "(Ljava/lang/String;)V" );
		references = m_index.getBehaviorReferences( source );
		assertThat( references, containsInAnyOrder(
			newBehaviorReferenceByConstructor( source, m_subClassAA.getName(), "()V" )
		) );
		
		// baseClass.getName()
		source = new MethodEntry( m_baseClass, "a", "()Ljava/lang/String;" );
		references = m_index.getBehaviorReferences( source );
		assertThat( references, containsInAnyOrder(
			newBehaviorReferenceByMethod( source, m_subClassAA.getName(), "a", "()Ljava/lang/String;" )
		) );
		
		// subclassAA.getName()
		source = new MethodEntry( m_subClassAA, "a", "()Ljava/lang/String;" );
		references = m_index.getBehaviorReferences( source );
		assertThat( references, containsInAnyOrder(
			newBehaviorReferenceByMethod( source, m_subClassAA.getName(), "a", "()V" )
		) );
	}
	
	@Test
	public void containsEntries( )
	{
		// classes
		assertThat( m_index.containsObfClass( m_baseClass ), is( true ) );
		assertThat( m_index.containsObfClass( m_subClassA ), is( true ) );
		assertThat( m_index.containsObfClass( m_subClassAA ), is( true ) );
		assertThat( m_index.containsObfClass( m_subClassB ), is( true ) );
		
		// fields
		assertThat( m_index.containsObfField( m_nameField ), is( true ) );
		assertThat( m_index.containsObfField( m_numThingsField ), is( true ) );
		
		// methods
		assertThat( m_index.containsObfMethod( new MethodEntry( m_baseClass, "a", "()Ljava/lang/String;" ) ), is( true ) );
		assertThat( m_index.containsObfMethod( new MethodEntry( m_baseClass, "a", "()V" ) ), is( true ) );
		assertThat( m_index.containsObfMethod( new MethodEntry( m_subClassAA, "a", "()Ljava/lang/String;" ) ), is( true ) );
		assertThat( m_index.containsObfMethod( new MethodEntry( m_subClassAA, "a", "()V" ) ), is( true ) );
		assertThat( m_index.containsObfMethod( new MethodEntry( m_subClassB, "a", "()V" ) ), is( true ) );
		assertThat( m_index.containsObfMethod( new MethodEntry( m_subClassB, "b", "()V" ) ), is( true ) );
	}
}
