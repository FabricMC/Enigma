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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

public class ClassMatching
{
	private Multimap<ClassIdentity,ClassIdentity> m_sourceClasses;
	private Multimap<ClassIdentity,ClassIdentity> m_matchedDestClasses;
	private List<ClassIdentity> m_unmatchedDestClasses;
	
	public ClassMatching( )
	{
		m_sourceClasses = ArrayListMultimap.create();
		m_matchedDestClasses = ArrayListMultimap.create();
		m_unmatchedDestClasses = Lists.newArrayList();
	}

	public void addSource( ClassIdentity c )
	{
		m_sourceClasses.put( c, c );
	}
	
	public void matchDestClass( ClassIdentity destClass )
	{
		Collection<ClassIdentity> matchedSourceClasses = m_sourceClasses.get( destClass );
		if( matchedSourceClasses.isEmpty() )
		{
			// no match
			m_unmatchedDestClasses.add( destClass );
		}
		else
		{
			// found a match
			m_matchedDestClasses.put( destClass, destClass );
			
			// DEBUG
			ClassIdentity sourceClass = matchedSourceClasses.iterator().next();
			assert( sourceClass.hashCode() == destClass.hashCode() );
			assert( sourceClass.equals( destClass ) );
		}
	}
	
	public void removeSource( ClassIdentity sourceClass )
	{
		m_sourceClasses.remove( sourceClass, sourceClass );
	}
	
	public void removeDest( ClassIdentity destClass )
	{
		m_matchedDestClasses.remove( destClass, destClass );
		m_unmatchedDestClasses.remove( destClass );
	}
	
	public List<ClassIdentity> getSourceClasses( )
	{
		return new ArrayList<ClassIdentity>( m_sourceClasses.values() );
	}
	
	public List<ClassIdentity> getDestClasses( )
	{
		List<ClassIdentity> classes = Lists.newArrayList();
		classes.addAll( m_matchedDestClasses.values() );
		classes.addAll( m_unmatchedDestClasses );
		return classes;
	}
	
	public BiMap<ClassIdentity,ClassIdentity> getUniqueMatches( )
	{
		BiMap<ClassIdentity,ClassIdentity> uniqueMatches = HashBiMap.create();
		for( ClassIdentity sourceClass : m_sourceClasses.keySet() )
		{
			Collection<ClassIdentity> matchedSourceClasses = m_sourceClasses.get( sourceClass );
			Collection<ClassIdentity> matchedDestClasses = m_matchedDestClasses.get( sourceClass );
			if( matchedSourceClasses.size() == 1 && matchedDestClasses.size() == 1 )
			{
				ClassIdentity matchedSourceClass = matchedSourceClasses.iterator().next();
				ClassIdentity matchedDestClass = matchedDestClasses.iterator().next();
				uniqueMatches.put( matchedSourceClass, matchedDestClass );
			}
		}
		return uniqueMatches;
	}
	
	public BiMap<List<ClassIdentity>,List<ClassIdentity>> getAmbiguousMatches( )
	{
		BiMap<List<ClassIdentity>,List<ClassIdentity>> ambiguousMatches = HashBiMap.create();
		for( ClassIdentity sourceClass : m_sourceClasses.keySet() )
		{
			Collection<ClassIdentity> matchedSourceClasses = m_sourceClasses.get( sourceClass );
			Collection<ClassIdentity> matchedDestClasses = m_matchedDestClasses.get( sourceClass );
			if( matchedSourceClasses.size() > 1 && matchedDestClasses.size() > 1 )
			{
				ambiguousMatches.put(
					new ArrayList<ClassIdentity>( matchedSourceClasses ),
					new ArrayList<ClassIdentity>( matchedDestClasses )
				);
			}
		}
		return ambiguousMatches;
	}
	
	public int getNumAmbiguousSourceMatches( )
	{
		int num = 0;
		for( Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : getAmbiguousMatches().entrySet() )
		{
			num += entry.getKey().size();
		}
		return num;
	}
	
	public int getNumAmbiguousDestMatches( )
	{
		int num = 0;
		for( Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : getAmbiguousMatches().entrySet() )
		{
			num += entry.getValue().size();
		}
		return num;
	}
	
	public List<ClassIdentity> getUnmatchedSourceClasses( )
	{
		List<ClassIdentity> classes = Lists.newArrayList();
		for( ClassIdentity sourceClass : getSourceClasses() )
		{
			if( m_matchedDestClasses.get( sourceClass ).isEmpty() )
			{
				classes.add( sourceClass );
			}
		}
		return classes;
	}
	
	public List<ClassIdentity> getUnmatchedDestClasses( )
	{
		return new ArrayList<ClassIdentity>( m_unmatchedDestClasses );
	}
	
	public Map<String,Map.Entry<ClassIdentity,List<ClassIdentity>>> getConversionMap( )
	{
		Map<String,Map.Entry<ClassIdentity,List<ClassIdentity>>> conversion = Maps.newHashMap();
		for( Map.Entry<ClassIdentity,ClassIdentity> entry : getUniqueMatches().entrySet() )
		{
			conversion.put(
				entry.getKey().getClassEntry().getName(),
				new AbstractMap.SimpleEntry<ClassIdentity,List<ClassIdentity>>( entry.getKey(), Arrays.asList( entry.getValue() ) )
			);
		}
		for( Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : getAmbiguousMatches().entrySet() )
		{
			for( ClassIdentity sourceClass : entry.getKey() )
			{
				conversion.put(
					sourceClass.getClassEntry().getName(),
					new AbstractMap.SimpleEntry<ClassIdentity,List<ClassIdentity>>( sourceClass, entry.getValue() )
				);
			}
		}
		for( ClassIdentity sourceClass : getUnmatchedSourceClasses() )
		{
			conversion.put(
				sourceClass.getClassEntry().getName(),
				new AbstractMap.SimpleEntry<ClassIdentity,List<ClassIdentity>>( sourceClass, getUnmatchedDestClasses() )
			);
		}
		return conversion;
	}
	
	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		buf.append( String.format( "%12s%8s%8s\n", "", "Source", "Dest" ) );
		buf.append( String.format( "%12s%8d%8d\n", "Classes", getSourceClasses().size(), getDestClasses().size() ) );
		buf.append( String.format( "%12s%8d%8d\n", "Unique", getUniqueMatches().size(), getUniqueMatches().size() ) );
		buf.append( String.format( "%12s%8d%8d\n", "Ambiguous", getNumAmbiguousSourceMatches(), getNumAmbiguousDestMatches() ) );
		buf.append( String.format( "%12s%8d%8d\n", "Unmatched", getUnmatchedSourceClasses().size(), getUnmatchedDestClasses().size() ) );
		return buf.toString();
	}
}
