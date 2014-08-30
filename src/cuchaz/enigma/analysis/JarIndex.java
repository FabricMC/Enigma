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
package cuchaz.enigma.analysis;

import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.Constants;
import cuchaz.enigma.bytecode.ClassRenamer;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class JarIndex
{
	private Set<ClassEntry> m_obfClassEntries;
	private Ancestries m_ancestries;
	private Map<Entry,Access> m_access;
	private Multimap<String,MethodEntry> m_methodImplementations;
	private Multimap<BehaviorEntry,EntryReference<BehaviorEntry,BehaviorEntry>> m_behaviorReferences;
	private Multimap<FieldEntry,EntryReference<FieldEntry,BehaviorEntry>> m_fieldReferences;
	private Multimap<String,String> m_innerClasses;
	private Map<String,String> m_outerClasses;
	private Set<String> m_anonymousClasses;
	private Map<MethodEntry,MethodEntry> m_bridgeMethods;
	
	public JarIndex( )
	{
		m_obfClassEntries = Sets.newHashSet();
		m_ancestries = new Ancestries();
		m_access = Maps.newHashMap();
		m_methodImplementations = HashMultimap.create();
		m_behaviorReferences = HashMultimap.create();
		m_fieldReferences = HashMultimap.create();
		m_innerClasses = HashMultimap.create();
		m_outerClasses = Maps.newHashMap();
		m_anonymousClasses = Sets.newHashSet();
		m_bridgeMethods = Maps.newHashMap();
	}
	
	public void indexJar( JarFile jar )
	{
		// step 1: read the class names
		for( ClassEntry classEntry : JarClassIterator.getClassEntries( jar ) )
		{
			if( classEntry.isInDefaultPackage() )
			{
				// move out of default package
				classEntry = new ClassEntry( Constants.NonePackage + "/" + classEntry.getName() );
			}
			m_obfClassEntries.add( classEntry );
		}
		
		// step 2: index method/field access
		for( CtClass c : JarClassIterator.classes( jar ) )
		{
			ClassRenamer.moveAllClassesOutOfDefaultPackage( c, Constants.NonePackage );
			ClassEntry classEntry = new ClassEntry( Descriptor.toJvmName( c.getName() ) );
			for( CtField field : c.getDeclaredFields() )
			{
				FieldEntry fieldEntry = new FieldEntry( classEntry, field.getName() );
				m_access.put( fieldEntry, Access.get( field ) );
			}
			for( CtBehavior behavior : c.getDeclaredBehaviors() )
			{
				MethodEntry methodEntry = new MethodEntry( classEntry, behavior.getName(), behavior.getSignature() );
				m_access.put( methodEntry, Access.get( behavior ) );
			}
		}
		
		// step 3: index the types, methods
		for( CtClass c : JarClassIterator.classes( jar ) )
		{
			ClassRenamer.moveAllClassesOutOfDefaultPackage( c, Constants.NonePackage );
			String className = Descriptor.toJvmName( c.getName() );
			m_ancestries.addSuperclass( className, Descriptor.toJvmName( c.getClassFile().getSuperclass() ) );
			for( String interfaceName : c.getClassFile().getInterfaces() )
			{
				m_ancestries.addInterface( className, Descriptor.toJvmName( interfaceName ) );
			}
			for( CtBehavior behavior : c.getDeclaredBehaviors() )
			{
				indexBehavior( behavior );
			}
		}
		
		// step 4: index inner classes and anonymous classes
		for( CtClass c : JarClassIterator.classes( jar ) )
		{
			ClassRenamer.moveAllClassesOutOfDefaultPackage( c, Constants.NonePackage );
			String outerClassName = findOuterClass( c );
			if( outerClassName != null )
			{
				String innerClassName = Descriptor.toJvmName( c.getName() );
				m_innerClasses.put( outerClassName, innerClassName );
				m_outerClasses.put( innerClassName, outerClassName );
				
				if( isAnonymousClass( c, outerClassName ) )
				{
					m_anonymousClasses.add( innerClassName );
					
					// DEBUG
					//System.out.println( "ANONYMOUS: " + outerClassName + "$" + innerClassName );
				}
				else
				{
					// DEBUG
					//System.out.println( "INNER: " + outerClassName + "$" + innerClassName );
				}
			}
		}
		
		// step 5: update other indices with inner class info
		Map<String,String> renames = Maps.newHashMap();
		for( Map.Entry<String,String> entry : m_outerClasses.entrySet() )
		{
			renames.put( entry.getKey(), entry.getValue() + "$" + new ClassEntry( entry.getKey() ).getSimpleName() );
		}
		renameClasses( renames );
		
		// step 5: update other indices with bridge method info
		renameMethods( m_bridgeMethods );
	}

	private void indexBehavior( CtBehavior behavior )
	{
		// get the method entry
		String className = Descriptor.toJvmName( behavior.getDeclaringClass().getName() );
		final BehaviorEntry thisEntry;
		if( behavior instanceof CtMethod )
		{
			MethodEntry methodEntry = new MethodEntry(
				new ClassEntry( className ),
				behavior.getName(),
				behavior.getSignature()
			);
			thisEntry = methodEntry;
			
			// index implementation
			m_methodImplementations.put( className, methodEntry );
			
			// look for bridge methods
			CtMethod bridgedMethod = getBridgedMethod( (CtMethod)behavior );
			if( bridgedMethod != null )
			{
				MethodEntry bridgedMethodEntry = new MethodEntry(
					new ClassEntry( className ),
					bridgedMethod.getName(),
					bridgedMethod.getSignature()
				);
				m_bridgeMethods.put( bridgedMethodEntry, methodEntry );
			}
		}
		else if( behavior instanceof CtConstructor )
		{
			boolean isStatic = behavior.getName().equals( "<clinit>" );
			if( isStatic )
			{
				thisEntry = new ConstructorEntry( new ClassEntry( className ) );
			}
			else
			{
				thisEntry = new ConstructorEntry( new ClassEntry( className ), behavior.getSignature() );
			}
		}
		else
		{
			throw new IllegalArgumentException( "behavior must be a method or a constructor!" );
		}
		
		// index method calls
		try
		{
			behavior.instrument( new ExprEditor( )
			{
				@Override
				public void edit( MethodCall call )
				{
					String className = Descriptor.toJvmName( call.getClassName() );
					MethodEntry calledMethodEntry = new MethodEntry(
						new ClassEntry( className ),
						call.getMethodName(),
						call.getSignature()
					);
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledMethodEntry,
						thisEntry
					);
					m_behaviorReferences.put( calledMethodEntry, reference );
				}
				
				@Override
				public void edit( FieldAccess call )
				{
					String className = Descriptor.toJvmName( call.getClassName() );
					FieldEntry calledFieldEntry = new FieldEntry(
						new ClassEntry( className ),
						call.getFieldName()
					);
					EntryReference<FieldEntry,BehaviorEntry> reference = new EntryReference<FieldEntry,BehaviorEntry>(
						calledFieldEntry,
						thisEntry
					);
					m_fieldReferences.put( calledFieldEntry, reference );
				}
				
				@Override
				public void edit( ConstructorCall call )
				{
					// TODO: save isSuper in the reference somehow
					boolean isSuper = call.getMethodName().equals( "super" );
					
					String className = Descriptor.toJvmName( call.getClassName() );
					ConstructorEntry calledConstructorEntry = new ConstructorEntry(
						new ClassEntry( className ),
						call.getSignature()
					);
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledConstructorEntry,
						thisEntry
					);
					m_behaviorReferences.put( calledConstructorEntry, reference );
				}
				
				@Override
				public void edit( NewExpr call )
				{
					String className = Descriptor.toJvmName( call.getClassName() );
					ConstructorEntry calledConstructorEntry = new ConstructorEntry(
						new ClassEntry( className ),
						call.getSignature()
					);
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledConstructorEntry,
						thisEntry
					);
					m_behaviorReferences.put( calledConstructorEntry, reference );
				}
			} );
		}
		catch( CannotCompileException ex )
		{
			throw new Error( ex );
		}
	}
	

	private CtMethod getBridgedMethod( CtMethod method )
	{
		// bridge methods just call another method, cast it to the return type, and return the result
		// let's see if we can detect this scenario
		
		// skip non-synthetic methods
		if( ( method.getModifiers() & AccessFlag.SYNTHETIC ) == 0 )
		{
			return null;
		}
					
		// get all the called methods
		final List<MethodCall> methodCalls = Lists.newArrayList();
		try
		{
			method.instrument( new ExprEditor( )
			{
				@Override
				public void edit( MethodCall call )
				{
					methodCalls.add( call );
				}
			} );
		}
		catch( CannotCompileException ex )
		{
			// this is stupid... we're not even compiling anything
			throw new Error( ex );
		}
		
		// is there just one?
		if( methodCalls.size() != 1 )
		{
			return null;
		}
		MethodCall call = methodCalls.get( 0 );
		
		try
		{
			// we have a bridge method!
			return call.getMethod();
		}
		catch( NotFoundException ex )
		{
			// can't find the type? not a bridge method
			return null;
		}
	}
	
	private String findOuterClass( CtClass c )
	{
		// inner classes:
		//    have constructors that can (illegally) set synthetic fields
		//    the outer class is the only class that calls constructors
		
		// use the synthetic fields to find the synthetic constructors
		for( CtConstructor constructor : c.getDeclaredConstructors() )
		{
			if( !isIllegalConstructor( constructor ) )
			{
				continue;
			}
			
			// who calls this constructor?
			Set<ClassEntry> callerClasses = Sets.newHashSet();
			ConstructorEntry constructorEntry = new ConstructorEntry(
				new ClassEntry( Descriptor.toJvmName( c.getName() ) ),
				constructor.getMethodInfo().getDescriptor()
			);
			for( EntryReference<BehaviorEntry,BehaviorEntry> reference : getBehaviorReferences( constructorEntry ) )
			{
				callerClasses.add( reference.context.getClassEntry() );
			}
			
			// is this called by exactly one class?
			if( callerClasses.size() == 1 )
			{
				return callerClasses.iterator().next().getName();
			}
			else if( callerClasses.size() > 1 )
			{
				System.out.println( "WARNING: Illegal constructor called by more than one class!" + callerClasses );
			}
		}
		
		return null;
	}
	
	@SuppressWarnings( "unchecked" )
	private boolean isIllegalConstructor( CtConstructor constructor )
	{
		// illegal constructors only set synthetic member fields, then call super()
		String className = constructor.getDeclaringClass().getName();
		
		// collect all the field accesses, constructor calls, and method calls
		final List<FieldAccess> illegalFieldWrites = Lists.newArrayList();
		final List<ConstructorCall> constructorCalls = Lists.newArrayList();
		final List<MethodCall> methodCalls = Lists.newArrayList();
		try
		{
			constructor.instrument( new ExprEditor( )
			{
				@Override
				public void edit( FieldAccess fieldAccess )
				{
					if( fieldAccess.isWriter() && constructorCalls.isEmpty() )
					{
						illegalFieldWrites.add( fieldAccess );
					}
				}
				
				@Override
				public void edit( ConstructorCall constructorCall )
				{
					constructorCalls.add( constructorCall );
				}
				
				@Override
				public void edit( MethodCall methodCall )
				{
					methodCalls.add( methodCall );
				}
			} );
		}
		catch( CannotCompileException ex )
		{
			// we're not compiling anything... this is stupid
			throw new Error( ex );
		}
		
		// method calls are not allowed
		if( !methodCalls.isEmpty() )
		{
			return false;
		}
		
		// is there only one constructor call?
		if( constructorCalls.size() != 1 )
		{
			return false;
		}
		
		// is the call to super?
		ConstructorCall constructorCall = constructorCalls.get( 0 );
		if( !constructorCall.getMethodName().equals( "super" ) )
		{
			return false;
		}
		
		// are there any illegal field writes?
		if( illegalFieldWrites.isEmpty() )
		{
			return false;
		}
		
		// are all the writes to synthetic fields?
		for( FieldAccess fieldWrite : illegalFieldWrites )
		{
			// all illegal writes have to be to the local class
			if( !fieldWrite.getClassName().equals( className ) )
			{
				System.err.println( String.format( "WARNING: illegal write to non-member field %s.%s", fieldWrite.getClassName(), fieldWrite.getFieldName() ) );
				return false;
			}
			
			// find the field
			FieldInfo fieldInfo = null;
			for( FieldInfo info : (List<FieldInfo>)constructor.getDeclaringClass().getClassFile().getFields() )
			{
				if( info.getName().equals( fieldWrite.getFieldName() ) )
				{
					fieldInfo = info;
					break;
				}
			}
			if( fieldInfo == null )
			{
				// field is in a superclass or something, can't be a local synthetic member
				return false;
			}
			
			// is this field synthetic?
			boolean isSynthetic = (fieldInfo.getAccessFlags() & AccessFlag.SYNTHETIC) != 0;
			if( !isSynthetic )
			{
				System.err.println( String.format( "WARNING: illegal write to non synthetic field %s.%s", className, fieldInfo.getName() ) );
				return false;
			}
		}
		
		// we passed all the tests!
		return true;
	}

	private boolean isAnonymousClass( CtClass c, String outerClassName )
	{
		String innerClassName = Descriptor.toJvmName( c.getName() );
		
		// anonymous classes:
		//    can't be abstract
		//    have only one constructor
		//    it's called exactly once by the outer class
		//    type of inner class not referenced anywhere in outer class
		
		// is absract?
		if( Modifier.isAbstract( c.getModifiers() ) )
		{
			return false;
		}
		
		// is there exactly one constructor?
		if( c.getDeclaredConstructors().length != 1 )
		{
			return false;
		}
		CtConstructor constructor = c.getDeclaredConstructors()[0];
		
		// is this constructor called exactly once?
		ConstructorEntry constructorEntry = new ConstructorEntry(
			new ClassEntry( innerClassName ),
			constructor.getMethodInfo().getDescriptor()
		);
		if( getBehaviorReferences( constructorEntry ).size() != 1 )
		{
			return false;
		}
		
		// TODO: check outer class doesn't reference type
		// except this is hard because we can't just load the outer class now
		// we'd have to pre-index those references in the JarIndex
		
		return true;
	}
	
	public Set<ClassEntry> getObfClassEntries( )
	{
		return m_obfClassEntries;
	}
	
	public Ancestries getAncestries( )
	{
		return m_ancestries;
	}
	
	public Access getAccess( Entry entry )
	{
		return m_access.get( entry );
	}
	
	public boolean isMethodImplemented( MethodEntry methodEntry )
	{
		Collection<MethodEntry> implementations = m_methodImplementations.get( methodEntry.getClassName() );
		if( implementations == null )
		{
			return false;
		}
		return implementations.contains( methodEntry );
	}

	public ClassInheritanceTreeNode getClassInheritance( Translator deobfuscatingTranslator, ClassEntry obfClassEntry )
	{
		// get the root node
		List<String> ancestry = Lists.newArrayList();
		ancestry.add( obfClassEntry.getName() );
		ancestry.addAll( m_ancestries.getAncestry( obfClassEntry.getName() ) );
		ClassInheritanceTreeNode rootNode = new ClassInheritanceTreeNode( deobfuscatingTranslator, ancestry.get( ancestry.size() - 1 ) );
		
		// expand all children recursively
		rootNode.load( m_ancestries, true );
		
		return rootNode;
	}
	
	public ClassImplementationsTreeNode getClassImplementations( Translator deobfuscatingTranslator, ClassEntry obfClassEntry )
	{
		ClassImplementationsTreeNode node = new ClassImplementationsTreeNode( deobfuscatingTranslator, obfClassEntry );
		node.load( m_ancestries );
		return node;
	}
	
	public MethodInheritanceTreeNode getMethodInheritance( Translator deobfuscatingTranslator, MethodEntry obfMethodEntry )
	{
		// travel to the ancestor implementation
		String baseImplementationClassName = obfMethodEntry.getClassName();
		for( String ancestorClassName : m_ancestries.getAncestry( obfMethodEntry.getClassName() ) )
		{
			MethodEntry ancestorMethodEntry = new MethodEntry(
				new ClassEntry( ancestorClassName ),
				obfMethodEntry.getName(),
				obfMethodEntry.getSignature()
			);
			if( isMethodImplemented( ancestorMethodEntry ) )
			{
				baseImplementationClassName = ancestorClassName;
			}
		}
		
		// make a root node at the base
		MethodEntry methodEntry = new MethodEntry(
			new ClassEntry( baseImplementationClassName ),
			obfMethodEntry.getName(),
			obfMethodEntry.getSignature()
		);
		MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(
			deobfuscatingTranslator,
			methodEntry,
			isMethodImplemented( methodEntry )
		);
		
		// expand the full tree
		rootNode.load( this, true );
		
		return rootNode;
	}
	
	public MethodImplementationsTreeNode getMethodImplementations( Translator deobfuscatingTranslator, MethodEntry obfMethodEntry )
	{
		MethodEntry interfaceMethodEntry;
		
		// is this method on an interface?
		if( m_ancestries.isInterface( obfMethodEntry.getClassName() ) )
		{
			interfaceMethodEntry = obfMethodEntry;
		}
		else
		{
			// get the interface class
			List<MethodEntry> methodInterfaces = Lists.newArrayList();
			for( String interfaceName : m_ancestries.getInterfaces( obfMethodEntry.getClassName() ) )
			{
				// is this method defined in this interface?
				MethodEntry methodInterface = new MethodEntry(
					new ClassEntry( interfaceName ),
					obfMethodEntry.getName(),
					obfMethodEntry.getSignature()
				);
				if( isMethodImplemented( methodInterface ) )
				{
					methodInterfaces.add( methodInterface );
				}
			}
			if( methodInterfaces.isEmpty() )
			{
				return null;
			}
			if( methodInterfaces.size() > 1 )
			{
				throw new Error( "Too many interfaces define this method! This is not yet supported by Enigma!" );
			}
			interfaceMethodEntry = methodInterfaces.get( 0 );
		}
		
		MethodImplementationsTreeNode rootNode = new MethodImplementationsTreeNode( deobfuscatingTranslator, interfaceMethodEntry );
		rootNode.load( this );
		return rootNode;
	}
	
	public Set<MethodEntry> getRelatedMethodImplementations( MethodEntry obfMethodEntry )
	{
		Set<MethodEntry> methodEntries = Sets.newHashSet();
		getRelatedMethodImplementations( methodEntries, getMethodInheritance( null, obfMethodEntry ) );
		return methodEntries;
	}
	
	private void getRelatedMethodImplementations( Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node )
	{
		MethodEntry methodEntry = node.getMethodEntry();
		if( isMethodImplemented( methodEntry ) )
		{
			// collect the entry
			methodEntries.add( methodEntry );
		}
		
		// look at interface methods too
		getRelatedMethodImplementations( methodEntries, getMethodImplementations( null, methodEntry ) );
		
		// recurse
		for( int i=0; i<node.getChildCount(); i++ )
		{
			getRelatedMethodImplementations( methodEntries, (MethodInheritanceTreeNode)node.getChildAt( i ) );
		}
	}
	
	private void getRelatedMethodImplementations( Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node )
	{
		MethodEntry methodEntry = node.getMethodEntry();
		if( isMethodImplemented( methodEntry ) )
		{
			// collect the entry
			methodEntries.add( methodEntry );
		}
		
		// recurse
		for( int i=0; i<node.getChildCount(); i++ )
		{
			getRelatedMethodImplementations( methodEntries, (MethodImplementationsTreeNode)node.getChildAt( i ) );
		}
	}

	public Collection<EntryReference<FieldEntry,BehaviorEntry>> getFieldReferences( FieldEntry fieldEntry )
	{
		return m_fieldReferences.get( fieldEntry );
	}
	
	public Collection<EntryReference<BehaviorEntry,BehaviorEntry>> getBehaviorReferences( BehaviorEntry behaviorEntry )
	{
		return m_behaviorReferences.get( behaviorEntry );
	}

	public Collection<String> getInnerClasses( String obfOuterClassName )
	{
		return m_innerClasses.get( obfOuterClassName );
	}
	
	public String getOuterClass( String obfInnerClassName )
	{
		return m_outerClasses.get( obfInnerClassName );
	}
	
	public boolean isAnonymousClass( String obfInnerClassName )
	{
		return m_anonymousClasses.contains( obfInnerClassName );
	}
	
	public MethodEntry getBridgeMethod( MethodEntry methodEntry )
	{
		return m_bridgeMethods.get( methodEntry );
	}
	
	private void renameClasses( Map<String,String> renames )
	{
		// rename class entries
		Set<ClassEntry> obfClassEntries = Sets.newHashSet();
		for( ClassEntry classEntry : m_obfClassEntries )
		{
			if( renames.containsKey( classEntry.getName() ) )
			{
				obfClassEntries.add( new ClassEntry( renames.get( classEntry.getName() ) ) );
			}
			else
			{
				obfClassEntries.add( classEntry );
			}
		}
		m_obfClassEntries = obfClassEntries;
		
		// rename others
		m_ancestries.renameClasses( renames );
		renameClassesInMultimap( renames, m_methodImplementations );
		renameClassesInMultimap( renames, m_behaviorReferences );
		renameClassesInMultimap( renames, m_fieldReferences );
	}
	
	private void renameMethods( Map<MethodEntry,MethodEntry> renames )
	{
		renameMethodsInMultimap( renames, m_methodImplementations );
		renameMethodsInMultimap( renames, m_behaviorReferences );
		renameMethodsInMultimap( renames, m_fieldReferences );
	}
	
	private <Key,Val> void renameClassesInMultimap( Map<String,String> renames, Multimap<Key,Val> map )
	{
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		for( Map.Entry<Key,Val> entry : map.entries() )
		{
			entriesToAdd.add( new AbstractMap.SimpleEntry<Key,Val>(
				renameClassesInThing( renames, entry.getKey() ),
				renameClassesInThing( renames, entry.getValue() )
			) );
		}
		map.clear();
		for( Map.Entry<Key,Val> entry : entriesToAdd )
		{
			map.put( entry.getKey(), entry.getValue() );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	private <T> T renameClassesInThing( Map<String,String> renames, T thing )
	{
		if( thing instanceof String )
		{
			String stringEntry = (String)thing;
			if( renames.containsKey( stringEntry ) )
			{
				return (T)renames.get( stringEntry );
			}
		}
		else if( thing instanceof ClassEntry )
		{
			ClassEntry classEntry = (ClassEntry)thing;
			return (T)new ClassEntry( renameClassesInThing( renames, classEntry.getClassName() ) );
		}
		else if( thing instanceof FieldEntry )
		{
			FieldEntry fieldEntry = (FieldEntry)thing;
			return (T)new FieldEntry(
				renameClassesInThing( renames, fieldEntry.getClassEntry() ),
				fieldEntry.getName()
			);
		}
		else if( thing instanceof ConstructorEntry )
		{
			ConstructorEntry constructorEntry = (ConstructorEntry)thing;
			return (T)new ConstructorEntry(
				renameClassesInThing( renames, constructorEntry.getClassEntry() ),
				constructorEntry.getSignature()
			);
		}
		else if( thing instanceof MethodEntry )
		{
			MethodEntry methodEntry = (MethodEntry)thing;
			return (T)new MethodEntry(
				renameClassesInThing( renames, methodEntry.getClassEntry() ),
				methodEntry.getName(),
				methodEntry.getSignature()
			);
		}
		else if( thing instanceof ArgumentEntry )
		{
			ArgumentEntry argumentEntry = (ArgumentEntry)thing;
			return (T)new ArgumentEntry(
				renameClassesInThing( renames, argumentEntry.getMethodEntry() ),
				argumentEntry.getIndex(),
				argumentEntry.getName()
			);
		}
		else if( thing instanceof EntryReference )
		{
			EntryReference<Entry,Entry> reference = (EntryReference<Entry,Entry>)thing;
			reference.entry = renameClassesInThing( renames, reference.entry );
			reference.context = renameClassesInThing( renames, reference.context );
			return thing;
		}
		else
		{
			throw new Error( "Not an entry: " + thing );
		}
		
		return thing;
	}
	
	private <Key,Val> void renameMethodsInMultimap( Map<MethodEntry,MethodEntry> renames, Multimap<Key,Val> map )
	{
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		Iterator<Map.Entry<Key,Val>> iter = map.entries().iterator();
		while( iter.hasNext() )
		{
			Map.Entry<Key,Val> entry = iter.next();
			iter.remove();
			entriesToAdd.add( new AbstractMap.SimpleEntry<Key,Val>(
				renameMethodsInThing( renames, entry.getKey() ),
				renameMethodsInThing( renames, entry.getValue() )
			) );
		}
		for( Map.Entry<Key,Val> entry : entriesToAdd )
		{
			map.put( entry.getKey(), entry.getValue() );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	private <T> T renameMethodsInThing( Map<MethodEntry,MethodEntry> renames, T thing )
	{
		if( thing instanceof MethodEntry )
		{
			MethodEntry methodEntry = (MethodEntry)thing;
			MethodEntry newMethodEntry = renames.get( methodEntry );
			if( newMethodEntry != null )
			{
				return (T)new MethodEntry(
					methodEntry.getClassEntry(),
					newMethodEntry.getName(),
					methodEntry.getSignature()
				);
			}
			return thing;
		}
		else if( thing instanceof ArgumentEntry )
		{
			ArgumentEntry argumentEntry = (ArgumentEntry)thing;
			return (T)new ArgumentEntry(
				renameMethodsInThing( renames, argumentEntry.getMethodEntry() ),
				argumentEntry.getIndex(),
				argumentEntry.getName()
			);
		}
		else if( thing instanceof EntryReference )
		{
			EntryReference<Entry,Entry> reference = (EntryReference<Entry,Entry>)thing;
			reference.entry = renameMethodsInThing( renames, reference.entry );
			reference.context = renameMethodsInThing( renames, reference.context );
			return thing;
		}
		return thing;
	}
}
