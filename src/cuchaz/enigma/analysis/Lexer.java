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

import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.Token;
import jsyntaxpane.TokenType;
import jsyntaxpane.lexers.JavaLexer;

public class Lexer implements Iterable<Token>
{
	private SyntaxDocument m_doc;
	private Iterator<Token> m_iter;
	
	public Lexer( CharSequence source )
	{
		m_doc = new SyntaxDocument( new JavaLexer() );
		m_doc.append( source.toString() );
		m_iter = m_doc.getTokens( 0, m_doc.getLength() );
	}
	
	@Override
	public Iterator<Token> iterator( )
	{
		return m_iter;
	}
	
	public String getText( Token token )
	{
		return token.getString( m_doc );
	}
	
	public Token getFirstIdentifier( )
	{
		for( Token token : this )
		{
			if( token.type == TokenType.IDENTIFIER )
			{
				return token;
			}
		}
		return null;
	}
	
	public Token getFirstIdentifierMatching( CharSequence val )
	{
		for( Token token : this )
		{
			if( token.type == TokenType.IDENTIFIER && getText( token ).equals( val.toString() ) )
			{
				return token;
			}
		}
		return null;
	}
	
	public Token getLastIdentifier( )
	{
		Token lastToken = null;
		for( Token token : this )
		{
			if( token.type == TokenType.IDENTIFIER )
			{
				lastToken = token;
			}
		}
		return lastToken;
	}
}
