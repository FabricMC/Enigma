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
import java.util.Iterator;
import java.util.Map;

import jsyntaxpane.Token;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.mapping.Entry;

public class SourceIndex implements Iterable<Map.Entry<Entry,Token>>
{
	private Multimap<Entry,Token> m_entryToTokens;
	
	public SourceIndex( )
	{
		m_entryToTokens = HashMultimap.create();
	}
	
	public void add( Entry entry, Token token )
	{
		m_entryToTokens.put( entry, token );
	}
	
	public Iterator<Map.Entry<Entry,Token>> iterator( )
	{
		return m_entryToTokens.entries().iterator();
	}
	
	public Collection<Token> tokens( )
	{
		return m_entryToTokens.values();
	}
	
	public Entry getEntry( Token token )
	{
		// linear search is fast enough for now
		for( Map.Entry<Entry,Token> entry : this )
		{
			if( entry.getValue().equals( token ) )
			{
				return entry.getKey();
			}
		}
		return null;
	}
	
	public Map.Entry<Entry,Token> getEntry( int pos )
	{
		// linear search is fast enough for now
		for( Map.Entry<Entry,Token> entry : this )
		{
			Token token = entry.getValue();
			if( pos >= token.start && pos <= token.end() )
			{
				return entry;
			}
		}
		return null;
	}
	
	public Collection<Token> getTokens( Entry entry )
	{
		return m_entryToTokens.get( entry );
	}
}
