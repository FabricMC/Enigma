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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.java.ast.AstNode;

import cuchaz.enigma.mapping.Entry;

public class SourceIndex
{
	private String m_source;
	private TreeMap<Token,Entry> m_tokens;
	private List<Integer> m_lineOffsets;
	
	public SourceIndex( String source )
	{
		m_source = source;
		m_tokens = Maps.newTreeMap();
		m_lineOffsets = Lists.newArrayList();
		
		// count the lines
		m_lineOffsets.add( 0 );
		for( int i=0; i<source.length(); i++ )
		{
			if( source.charAt( i ) == '\n' )
			{
				m_lineOffsets.add( i + 1 );
			}
		}
	}
	
	public String getSource( )
	{
		return m_source;
	}
	
	public Token getToken( AstNode node )
	{
		// get a token for this node's region
		Region region = node.getRegion();
		if( region.getBeginLine() == 0 || region.getEndLine() == 0 )
		{
			throw new IllegalArgumentException( "Invalid region: " + region );
		}
		return new Token(
			toPos( region.getBeginLine(), region.getBeginColumn() ),
			toPos( region.getEndLine(), region.getEndColumn() )
		);
	}
	
	public void add( AstNode node, Entry entry )
	{
		m_tokens.put( getToken( node ), entry );
	}
	
	public void add( Token token, Entry entry )
	{
		m_tokens.put( token, entry );
	}
	
	public Token getToken( int pos )
	{
		Map.Entry<Token,Entry> mapEntry = m_tokens.floorEntry( new Token( pos, pos ) );
		if( mapEntry == null )
		{
			return null;
		}
		Token token = mapEntry.getKey();
		if( token.contains( pos ) )
		{
			return token;
		}
		return null;
	}
	
	public Entry getEntry( Token token )
	{
		if( token == null )
		{
			return null;
		}
		return m_tokens.get( token );
	}
	
	public Iterable<Token> tokens( )
	{
		return m_tokens.keySet();
	}
	
	private int toPos( int line, int col )
	{
		// line and col are 1-based
		return m_lineOffsets.get( line - 1 ) + col - 1;
	}
}
