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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jsyntaxpane.Token;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cuchaz.enigma.mapping.Entry;

public class SourceIndex implements Iterable<Map.Entry<Entry,Token>>
{
	private BiMap<Entry,Token> m_entryToToken;
	private BiMap<Token,Entry> m_tokenToEntry;
	
	public SourceIndex( )
	{
		m_entryToToken = HashBiMap.create();
		m_tokenToEntry = m_entryToToken.inverse();
	}
	
	public void add( Entry entry, Token token )
	{
		m_entryToToken.put( entry, token );
	}
	
	public Iterator<Map.Entry<Entry,Token>> iterator( )
	{
		return m_entryToToken.entrySet().iterator();
	}
	
	public Set<Token> tokens( )
	{
		return m_entryToToken.values();
	}
	
	public Entry getEntry( Token token )
	{
		return m_tokenToEntry.get( token );
	}
	
	public Entry getEntry( int pos )
	{
		// linear search is fast enough for now
		for( Map.Entry<Entry,Token> entry : this )
		{
			Token token = entry.getValue();
			if( pos >= token.start && pos <= token.end() )
			{
				return entry.getKey();
			}
		}
		return null;
	}
	
	public Token getToken( Entry entry )
	{
		return m_entryToToken.get( entry );
	}
}
