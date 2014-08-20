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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

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
	private Set<String> m_obfClassNames;
	private Ancestries m_ancestries;
	private Multimap<String,MethodEntry> m_methodImplementations;
	private Multimap<BehaviorEntry,EntryReference<BehaviorEntry,BehaviorEntry>> m_behaviorReferences;
	private Multimap<FieldEntry,EntryReference<FieldEntry,BehaviorEntry>> m_fieldReferences;
	private Multimap<String,String> m_innerClasses;
	private Map<String,String> m_outerClasses;
	private Set<String> m_anonymousClasses;
	
	public JarIndex( )
	{
		m_obfClassNames = Sets.newHashSet();
		m_ancestries = new Ancestries();
		m_methodImplementations = HashMultimap.create();
		m_behaviorReferences = HashMultimap.create();
		m_fieldReferences = HashMultimap.create();
		m_innerClasses = HashMultimap.create();
		m_outerClasses = Maps.newHashMap();
		m_anonymousClasses = Sets.newHashSet();
	}
	
	public void indexJar( JarFile jar )
	{
		// pass 1: read the class names
		for( JarEntry entry : JarClassIterator.getClassEntries( jar ) )
		{
			String className = entry.getName().substring( 0, entry.getName().length() - 6 );
			m_obfClassNames.add( Descriptor.toJvmName( className ) );
		}
		
		// pass 2: index the types, methods
		for( CtClass c : JarClassIterator.classes( jar ) )
		{
			m_ancestries.addSuperclass( c.getName(), c.getClassFile().getSuperclass() );
			for( CtBehavior behavior : c.getDeclaredBehaviors() )
			{
				indexBehavior( behavior );
			}
		}
		
		// pass 2: index inner classes and anonymous classes
		for( CtClass c : JarClassIterator.classes( jar ) )
		{
			String outerClassName = findOuterClass( c );
			if( outerClassName != null )// /* TEMP */ && false )
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
		
		// step 3: update other indicies with inner class info
		Map<String,String> renames = Maps.newHashMap();
		for( Map.Entry<String,String> entry : m_outerClasses.entrySet() )
		{
			renames.put( entry.getKey(), entry.getValue() + "$" + entry.getKey() );
		}
		renameClasses( renames );
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
		}
		else if( behavior instanceof CtConstructor )
		{
			thisEntry = new ConstructorEntry(
				new ClassEntry( className ),
				behavior.getSignature()
			);
		}
		else
		{
			throw new IllegalArgumentException( "behavior must be a method or a constructor!" );
		}
		
		// index method calls
		try
		{
			final Multiset<Entry> callNumbers = HashMultiset.create();
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
					callNumbers.add( calledMethodEntry );
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledMethodEntry,
						thisEntry,
						callNumbers.count( calledMethodEntry ) - 1						
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
					callNumbers.add( calledFieldEntry );
					EntryReference<FieldEntry,BehaviorEntry> reference = new EntryReference<FieldEntry,BehaviorEntry>(
						calledFieldEntry,
						thisEntry,
						callNumbers.count( calledFieldEntry ) - 1
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
					callNumbers.add( calledConstructorEntry );
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledConstructorEntry,
						thisEntry,
						callNumbers.count( calledConstructorEntry ) - 1						
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
					callNumbers.add( calledConstructorEntry );
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledConstructorEntry,
						thisEntry,
						callNumbers.count( calledConstructorEntry ) - 1						
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
				// TEMP
				System.out.println( "WARNING: Illegal class called by more than one class!" + callerClasses );
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
	
	public Set<String> getObfClassNames( )
	{
		return m_obfClassNames;
	}
	
	public Ancestries getAncestries( )
	{
		return m_ancestries;
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
	
	private void renameClasses( Map<String,String> renames )
	{
		m_ancestries.renameClasses( renames );
		renameMultimap( renames, m_methodImplementations );
		renameMultimap( renames, m_behaviorReferences );
		renameMultimap( renames, m_fieldReferences );
	}
	
	private <Key,Val> void renameMultimap( Map<String,String> renames, Multimap<Key,Val> map )
	{
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		Iterator<Map.Entry<Key,Val>> iter = map.entries().iterator();
		while( iter.hasNext() )
		{
			Map.Entry<Key,Val> entry = iter.next();
			iter.remove();
			entriesToAdd.add( new AbstractMap.SimpleEntry<Key,Val>(
				renameThing( renames, entry.getKey() ),
				renameThing( renames, entry.getValue() )
			) );
		}
		for( Map.Entry<Key,Val> entry : entriesToAdd )
		{
			map.put( entry.getKey(), entry.getValue() );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	private <T> T renameThing( Map<String,String> renames, T thing )
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
			return (T)new ClassEntry( renameThing( renames, classEntry.getClassName() ) );
		}
		else if( thing instanceof FieldEntry )
		{
			FieldEntry fieldEntry = (FieldEntry)thing;
			return (T)new FieldEntry(
				renameThing( renames, fieldEntry.getClassEntry() ),
				fieldEntry.getName()
			);
		}
		else if( thing instanceof ConstructorEntry )
		{
			ConstructorEntry constructorEntry = (ConstructorEntry)thing;
			return (T)new ConstructorEntry(
				renameThing( renames, constructorEntry.getClassEntry() ),
				constructorEntry.getSignature()
			);
		}
		else if( thing instanceof MethodEntry )
		{
			MethodEntry methodEntry = (MethodEntry)thing;
			return (T)new MethodEntry(
				renameThing( renames, methodEntry.getClassEntry() ),
				methodEntry.getName(),
				methodEntry.getSignature()
			);
		}
		else if( thing instanceof ArgumentEntry )
		{
			ArgumentEntry argumentEntry = (ArgumentEntry)thing;
			return (T)new ArgumentEntry(
				renameThing( renames, argumentEntry.getMethodEntry() ),
				argumentEntry.getIndex(),
				argumentEntry.getName()
			);
		}
		else if( thing instanceof EntryReference )
		{
			EntryReference<Entry,Entry> reference = (EntryReference<Entry,Entry>)thing;
			reference.entry = renameThing( renames, reference.entry );
			reference.context = renameThing( renames, reference.context );
			return thing;
		}
		else
		{
			throw new Error( "Not an entry: " + thing );
		}
		
		return thing;
	}
}
