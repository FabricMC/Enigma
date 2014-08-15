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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import com.beust.jcommander.internal.Lists;

import cuchaz.enigma.Constants;

public class JarClassIterator implements Iterator<CtClass>
{
	private JarFile m_jar;
	private Iterator<JarEntry> m_iter;
	
	public JarClassIterator( JarFile jar )
	{
		this( jar, getClassEntries( jar ) );
	}
	
	public JarClassIterator( JarFile jar, List<JarEntry> entries )
	{
		m_jar = jar;
		m_iter = entries.iterator();
	}
	
	@Override
	public boolean hasNext( )
	{
		return m_iter.hasNext();
	}

	@Override
	public CtClass next( )
	{
		JarEntry entry = m_iter.next();
		try
		{
			// read the class into a buffer
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[Constants.KiB];
			int totalNumBytesRead = 0;
			InputStream in = m_jar.getInputStream( entry );
			while( in.available() > 0 )
			{
				int numBytesRead = in.read( buf );
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
			ClassPool classPool = new ClassPool();
			classPool.insertClassPath( new ByteArrayClassPath( className, bos.toByteArray() ) );
			return classPool.get( className );
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to read class: " + entry.getName() );
		}
		catch( NotFoundException ex )
		{
			throw new Error( "Unable to load class: " + entry.getName() );
		}
	}

	@Override
	public void remove( )
	{
		throw new UnsupportedOperationException();
	}
	
	public static List<JarEntry> getClassEntries( JarFile jar )
	{
		List<JarEntry> classes = Lists.newArrayList();
		Enumeration<JarEntry> entries = jar.entries();
		while( entries.hasMoreElements() )
		{
			JarEntry entry = entries.nextElement();
			
			// is this a class file?
			if( !entry.isDirectory() && entry.getName().endsWith( ".class" ) )
			{
				classes.add( entry );
			}
		}
		return classes;
	}
	
	public static Iterable<CtClass> classes( final JarFile jar )
	{
		return new Iterable<CtClass>( )
		{
			@Override
			public Iterator<CtClass> iterator( )
			{
				return new JarClassIterator( jar );
			}
		};
	}
}
