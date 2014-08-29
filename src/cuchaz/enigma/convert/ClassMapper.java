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
package cuchaz.enigma.convert;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import javassist.CtClass;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.analysis.JarClassIterator;
import cuchaz.enigma.mapping.ClassEntry;

public class ClassMapper
{
	private int m_numSourceClasses;
	private int m_numDestClasses;
	private Multimap<ClassIdentity,ClassIdentity> m_sourceClasses;
	private Multimap<ClassIdentity,ClassIdentity> m_destClasses;
	private List<ClassIdentity> m_unmatchedSourceClasses;
	private List<ClassIdentity> m_unmatchedDestClasses;
	private Map<ClassEntry,ClassIdentity> m_sourceKeyIndex;
	
	public static void main( String[] args )
	throws IOException
	{
		// TEMP
		JarFile fromJar = new JarFile( new File( "input/1.8-pre1.jar" ) );
		JarFile toJar = new JarFile( new File( "input/1.8-pre2.jar" ) );
		
		ClassMapper mapper = new ClassMapper( fromJar, toJar );
		System.out.println( String.format( "Mapped %d/%d source classes (%d unmatched) to %d/%d dest classes (%d unmatched)",
			mapper.m_sourceClasses.size(), mapper.m_numSourceClasses, mapper.m_unmatchedSourceClasses.size(),
			mapper.m_destClasses.size(), mapper.m_numDestClasses, mapper.m_unmatchedDestClasses.size()
		) );
	}
	
	public ClassMapper( JarFile sourceJar, JarFile destJar )
	{
		m_numSourceClasses = JarClassIterator.getClassEntries( sourceJar ).size();
		m_numDestClasses = JarClassIterator.getClassEntries( destJar ).size();
		
		// compute identities for the source classes
		m_sourceClasses = ArrayListMultimap.create();
		m_sourceKeyIndex = Maps.newHashMap();
		for( CtClass c : JarClassIterator.classes( sourceJar ) )
		{
			ClassIdentity sourceClass = new ClassIdentity( c );
			m_sourceClasses.put( sourceClass, sourceClass );
			m_sourceKeyIndex.put( sourceClass.getClassEntry(), sourceClass );
		}
		
		// match the dest classes to the source classes
		m_destClasses = ArrayListMultimap.create();
		m_unmatchedDestClasses = Lists.newArrayList();
		for( CtClass c : JarClassIterator.classes( destJar ) )
		{
			ClassIdentity destClass = new ClassIdentity( c );
			Collection<ClassIdentity> matchedSourceClasses = m_sourceClasses.get( destClass );
			if( matchedSourceClasses.isEmpty() )
			{
				// unmatched dest class
				m_unmatchedDestClasses.add( destClass );
			}
			else
			{
				ClassIdentity sourceClass = matchedSourceClasses.iterator().next();
				m_destClasses.put( sourceClass, destClass );
			}
		}

		// get unmatched source classes
		m_unmatchedSourceClasses = Lists.newArrayList();
		for( ClassIdentity sourceClass : m_sourceClasses.keySet() )
		{
			Collection<ClassIdentity> matchedSourceClasses = m_sourceClasses.get( sourceClass );
			Collection<ClassIdentity> matchedDestClasses = m_destClasses.get( sourceClass );
			if( matchedDestClasses.isEmpty() )
			{
				m_unmatchedSourceClasses.add( sourceClass );
			}
			else if( matchedDestClasses.size() > 1 )
			{
				// warn about identity collisions
				System.err.println( String.format( "WARNING: identity collision:\n\tSource: %s\n\t  Dest: %s",
					getClassEntries( matchedSourceClasses ),
					getClassEntries( matchedDestClasses )
				) );
			}
		}
	}
	
	public Map.Entry<Collection<ClassEntry>,Collection<ClassEntry>> getMapping( ClassEntry sourceEntry )
	{
		// TODO
		return null;
	}

	private Collection<ClassEntry> getClassEntries( Collection<ClassIdentity> classes )
	{
		List<ClassEntry> entries = Lists.newArrayList();
		for( ClassIdentity c : classes )
		{
			entries.add( c.getClassEntry() );
		}
		return entries;
	}
}
