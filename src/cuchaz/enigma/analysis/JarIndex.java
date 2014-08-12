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
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.Constants;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class JarIndex
{
	private Set<String> m_obfClassNames;
	private Ancestries m_ancestries;
	private Multimap<String,MethodEntry> m_methodImplementations;
	private Multimap<MethodEntry,MethodEntry> m_methodCalls;
	
	public JarIndex( JarFile jar )
	{
		m_obfClassNames = Sets.newHashSet();
		m_ancestries = new Ancestries();
		m_methodImplementations = HashMultimap.create();
		m_methodCalls = HashMultimap.create();
		
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
		final MethodEntry methodEntry = new MethodEntry(
			new ClassEntry( className ),
			behavior.getName(),
			behavior.getSignature()
		);
		
		// index implementation
		m_methodImplementations.put( className, methodEntry );
		
		// index method calls
		try
		{
			behavior.instrument( new ExprEditor( )
			{
				@Override
				public void edit( MethodCall call )
				{
					// is this a jar class?
					String className = Descriptor.toJvmName( call.getClassName() );
					if( !m_obfClassNames.contains( className ) )
					{
						return;
					}
					
					// make entry for the called method
					MethodEntry calledMethodEntry = new MethodEntry(
						new ClassEntry( className ),
						call.getMethodName(),
						call.getSignature()
					);
					m_methodCalls.put( calledMethodEntry, methodEntry );
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
		List<String> ancestry = m_ancestries.getAncestry( obfClassEntry.getName() );
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
	
	public Collection<MethodEntry> getMethodCallers( MethodEntry methodEntry )
	{
		return m_methodCalls.get( methodEntry );
	}
	
	private String getMethodKey( String name, String signature )
	{
		return name + signature;
	}
}
