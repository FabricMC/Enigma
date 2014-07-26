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

public class SourceIndex implements Iterable<Map.Entry<Object,Token>>
{
	private BiMap<Object,Token> m_entryToToken;
	private BiMap<Token,Object> m_tokenToEntry;
	
	public SourceIndex( )
	{
		m_entryToToken = HashBiMap.create();
		m_tokenToEntry = m_entryToToken.inverse();
	}
	
	public void add( Object entry, Token token )
	{
		m_entryToToken.put( entry, token );
	}
	
	public Iterator<Map.Entry<Object,Token>> iterator( )
	{
		return m_entryToToken.entrySet().iterator();
	}
	
	public Set<Token> tokens( )
	{
		return m_entryToToken.values();
	}
	
	public Object getEntry( Token token )
	{
		return m_tokenToEntry.get( token );
	}
	
	public Object getToken( Object entry )
	{
		return m_entryToToken.get( entry );
	}
}
