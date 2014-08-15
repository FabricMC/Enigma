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

import java.util.Collection;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

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
	private Multimap<Entry,Entry> m_methodCalls;
	private Multimap<FieldEntry,Entry> m_fieldCalls;
	private Multimap<String,String> m_innerClasses;
	private Map<String,String> m_outerClasses;
	
	public JarIndex( )
	{
		m_obfClassNames = Sets.newHashSet();
		m_ancestries = new Ancestries();
		m_methodImplementations = HashMultimap.create();
		m_methodCalls = HashMultimap.create();
		m_fieldCalls = HashMultimap.create();
		m_innerClasses = HashMultimap.create();
		m_outerClasses = Maps.newHashMap();
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
		
		// pass 2: index inner classes
		for( CtClass c : JarClassIterator.classes( jar ) )
		{
			String outerClassName = isInnerClass( c );
			if( outerClassName != null )
			{
				String innerClassName = Descriptor.toJvmName( c.getName() );
				m_innerClasses.put( outerClassName, innerClassName );
				m_outerClasses.put( innerClassName, outerClassName );
			}
		}
	}

	private void indexBehavior( CtBehavior behavior )
	{
		// get the method entry
		String className = Descriptor.toJvmName( behavior.getDeclaringClass().getName() );
		final Entry thisEntry;
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
					m_methodCalls.put( calledMethodEntry, thisEntry );
				}
				
				@Override
				public void edit( FieldAccess call )
				{
					String className = Descriptor.toJvmName( call.getClassName() );
					FieldEntry calledFieldEntry = new FieldEntry(
						new ClassEntry( className ),
						call.getFieldName()
					);
					m_fieldCalls.put( calledFieldEntry, thisEntry );
				}
				
				@Override
				public void edit( ConstructorCall call )
				{
					String className = Descriptor.toJvmName( call.getClassName() );
					ConstructorEntry calledConstructorEntry = new ConstructorEntry(
						new ClassEntry( className ),
						call.getSignature()
					);
					m_methodCalls.put( calledConstructorEntry, thisEntry );
				}
				
				@Override
				public void edit( NewExpr call )
				{
					String className = Descriptor.toJvmName( call.getClassName() );
					ConstructorEntry calledConstructorEntry = new ConstructorEntry(
						new ClassEntry( className ),
						call.getSignature()
					);
					m_methodCalls.put( calledConstructorEntry, thisEntry );
				}
			} );
		}
		catch( CannotCompileException ex )
		{
			throw new Error( ex );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	private String isInnerClass( CtClass c )
	{
		String innerClassName = Descriptor.toJvmName( c.getName() );
		
		// first, is this an anonymous class?
		// for anonymous classes:
		//    the outer class is always a synthetic field
		//    there's at least one constructor with the type of the synthetic field as an argument
		//    this constructor is called exactly once by the class of the synthetic field
		
		for( FieldInfo field : (List<FieldInfo>)c.getClassFile().getFields() )
		{
			boolean isSynthetic = (field.getAccessFlags() & AccessFlag.SYNTHETIC) != 0;
			if( !isSynthetic )
			{
				continue;
			}
			
			// skip non-class types
			if( !field.getDescriptor().startsWith( "L" ) )
			{
				continue;
			}
			
			// get the outer class from the field type
			String outerClassName = Descriptor.toJvmName( Descriptor.toClassName( field.getDescriptor() ) );
			
			// look for a constructor where this type is the first parameter
			CtConstructor targetConstructor = null;
			for( CtConstructor constructor : c.getDeclaredConstructors() )
			{
				String signature = Descriptor.getParamDescriptor( constructor.getMethodInfo().getDescriptor() );
				if( Descriptor.numOfParameters( signature ) < 1 )
				{
					continue;
				}
				
				// match the first parameter to the outer class
				Descriptor.Iterator iter = new Descriptor.Iterator( signature );
				int pos = iter.next();
				if( iter.isParameter() && signature.charAt( pos ) == 'L' )
				{
					String argumentDesc = signature.substring( pos, signature.indexOf(';', pos) + 1 );
					String argumentClassName = Descriptor.toJvmName( Descriptor.toClassName( argumentDesc ) );
					if( argumentClassName.equals( outerClassName ) )
					{
						// is this constructor called exactly once?
						ConstructorEntry constructorEntry = new ConstructorEntry(
							new ClassEntry( innerClassName ),
							constructor.getMethodInfo().getDescriptor()
						);
						if( this.getMethodCallers( constructorEntry ).size() == 1 )
						{
							targetConstructor = constructor;
							break;
						}
					}
				}
			}
			if( targetConstructor == null )
			{
				continue;
			}
			
			// yeah, this is an inner class
			return outerClassName;
		}
		
		return null;
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
	
	public Collection<Entry> getFieldCallers( FieldEntry fieldEntry )
	{
		return m_fieldCalls.get( fieldEntry );
	}
	
	public Collection<Entry> getMethodCallers( Entry entry )
	{
		return m_methodCalls.get( entry );
	}

	public Collection<String> getInnerClasses( String obfOuterClassName )
	{
		return m_innerClasses.get( obfOuterClassName );
	}
	
	public String getOuterClass( String obfInnerClassName )
	{
		return m_outerClasses.get( obfInnerClassName );
	}
}
