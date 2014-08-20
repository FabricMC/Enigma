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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.java.ast.Identifier;

import cuchaz.enigma.mapping.Entry;

public class SourceIndex
{
	private String m_source;
	private TreeMap<Token,EntryReference<Entry,Entry>> m_tokenToReference;
	private HashMap<EntryReference<Entry,Entry>,Token> m_referenceToToken;
	private Map<Entry,Token> m_declarationToToken;
	private List<Integer> m_lineOffsets;
	
	public SourceIndex( String source )
	{
		m_source = source;
		m_tokenToReference = Maps.newTreeMap();
		m_referenceToToken = Maps.newHashMap();
		m_declarationToToken = Maps.newHashMap();
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
	
	public Token getToken( Identifier node )
	{
		// get a token for this node's region
		Region region = node.getRegion();
		if( region.getBeginLine() == 0 || region.getEndLine() == 0 )
		{
			System.err.println( "WARNING: " + node.getNodeType() + " node has invalid region: " + region );
			return null;
		}
		Token token = new Token(
			toPos( region.getBeginLine(), region.getBeginColumn() ),
			toPos( region.getEndLine(), region.getEndColumn() )
		);
		
		// for tokens representing inner classes, make sure we only get the simple name
		int pos = node.getName().lastIndexOf( '$' );
		if( pos >= 0 )
		{
			token.end -= pos + 1;
		}
		
		// HACKHACK: sometimes node regions are off by one
		// I think this is a bug in Procyon, but it's easy to work around
		if( !Character.isJavaIdentifierStart( m_source.charAt( token.start ) ) )
		{
			token.start++;
			token.end++;
			if( !Character.isJavaIdentifierStart( m_source.charAt( token.start ) ) )
			{
				throw new IllegalArgumentException( "Region " + region + " does not describe valid token: '" + m_source.substring( token.start, token.end ) + "'" );
			}
		}
		
		return token;
	}
	
	public void addReference( Identifier node, EntryReference<Entry,Entry> deobfReference )
	{
		Token token = getToken( node );
		if( token != null )
		{
			m_tokenToReference.put( token, deobfReference );
			m_referenceToToken.put( deobfReference, token );
		}
	}
	
	public void addDeclaration( Identifier node, Entry deobfEntry )
	{
		Token token = getToken( node );
		if( token != null )
		{
			EntryReference<Entry,Entry> reference = new EntryReference<Entry,Entry>( deobfEntry );
			m_tokenToReference.put( token, reference );
			m_referenceToToken.put( reference, token );
			m_declarationToToken.put( deobfEntry, token );
		}
	}
	
	public Token getReferenceToken( int pos )
	{
		Token token = m_tokenToReference.floorKey( new Token( pos, pos ) );
		if( token.contains( pos ) )
		{
			return token;
		}
		return null;
	}
	
	public Token getReferenceToken( EntryReference<Entry,Entry> deobfReference )
	{
		return m_referenceToToken.get( deobfReference );
	}
	
	public EntryReference<Entry,Entry> getDeobfReference( Token token )
	{
		if( token == null )
		{
			return null;
		}
		return m_tokenToReference.get( token );
	}
	
	public Iterable<Token> referenceTokens( )
	{
		return m_tokenToReference.keySet();
	}
	
	public Iterable<Token> declarationTokens( )
	{
		return m_declarationToToken.values();
	}
	
	public Token getDeclarationToken( Entry deobfEntry )
	{
		return m_declarationToToken.get( deobfEntry );
	}
	
	private int toPos( int line, int col )
	{
		// line and col are 1-based
		return m_lineOffsets.get( line - 1 ) + col - 1;
	}
}
