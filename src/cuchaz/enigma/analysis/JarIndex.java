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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.Constants;
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
	
	public JarIndex( JarFile jar )
	{
		m_obfClassNames = Sets.newHashSet();
		m_ancestries = new Ancestries();
		m_methodImplementations = HashMultimap.create();
		m_methodCalls = HashMultimap.create();
		m_fieldCalls = HashMultimap.create();
		
		// read the class names
		Enumeration<JarEntry> enumeration = jar.entries();
		while( enumeration.hasMoreElements() )
		{
			JarEntry entry = enumeration.nextElement();
			
			// filter out non-classes
			if( entry.isDirectory() || !entry.getName().endsWith( ".class" ) )
			{
				continue;
			}
			
			String className = entry.getName().substring( 0, entry.getName().length() - 6 );
			m_obfClassNames.add( Descriptor.toJvmName( className ) );
		}
	}
	
	public void indexJar( InputStream in )
	throws IOException
	{
		ClassPool classPool = new ClassPool();
		
		ZipInputStream zin = new ZipInputStream( in );
		ZipEntry entry;
		while( ( entry = zin.getNextEntry() ) != null )
		{
			// filter out non-classes
			if( entry.isDirectory() || !entry.getName().endsWith( ".class" ) )
			{
				continue;
			}
			
			// read the class into a buffer
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[Constants.KiB];
			int totalNumBytesRead = 0;
			while( zin.available() > 0 )
			{
				int numBytesRead = zin.read( buf );
				if( numBytesRead < 0 )
				{
					break;
				}
				bos.write( buf, 0, numBytesRead );
				
				// sanity checking
				totalNumBytesRead += numBytesRead;
				if( totalNumBytesRead > Constants.MiB )
				{
					throw new Error( "Class file " + entry.getName() + " larger than 1 MiB! Something is wrong!" );
				}
			}
			
			// determine the class name (ie chop off the ".class")
			String className = Descriptor.toJavaName( entry.getName().substring( 0, entry.getName().length() - ".class".length() ) );
			
			// get a javassist handle for the class
			classPool.insertClassPath( new ByteArrayClassPath( className, bos.toByteArray() ) );
			try
			{
				CtClass c = classPool.get( className );
				m_ancestries.addSuperclass( c.getName(), c.getClassFile().getSuperclass() );
				for( CtBehavior behavior : c.getDeclaredBehaviors() )
				{
					indexBehavior( behavior );
				}
			}
			catch( NotFoundException ex )
			{
				throw new Error( "Unable to load class: " + className );
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
					
					// TEMP
					if( className.equals( "bgw" ) )
					{
						System.out.println( calledConstructorEntry + " called by " + thisEntry );
					}
					
					m_methodCalls.put( calledConstructorEntry, thisEntry );
				}
			} );
		}
		catch( CannotCompileException ex )
		{
			throw new Error( ex );
		}
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
		return isMethodImplemented( methodEntry.getClassName(), methodEntry.getName(), methodEntry.getSignature() );
	}
	
	public boolean isMethodImplemented( String className, String methodName, String methodSignature )
	{
		Collection<MethodEntry> implementations = m_methodImplementations.get( className );
		if( implementations == null )
		{
			return false;
		}
		return implementations.contains( getMethodKey( methodName, methodSignature ) );
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
			if( isMethodImplemented( ancestorClassName, obfMethodEntry.getName(), obfMethodEntry.getSignature() ) )
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
	
	private String getMethodKey( String name, String signature )
	{
		return name + signature;
	}
}
