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
package cuchaz.enigma.mapping;

import java.util.regex.Pattern;

public class NameValidator
{
	private static final String IdentifierPattern;
	private static final Pattern ClassPattern;
	
	static
	{
		// java allows all kinds of weird characters...
		StringBuilder startChars = new StringBuilder();
		StringBuilder partChars = new StringBuilder();
		for( int i = Character.MIN_CODE_POINT; i <= Character.MAX_CODE_POINT; i++ )
		{
			if( Character.isJavaIdentifierStart( i ) )
			{
				startChars.appendCodePoint( i );
			}
			if( Character.isJavaIdentifierPart( i ) )
			{
				partChars.appendCodePoint( i );
			}
		}
		
		IdentifierPattern = String.format( "[\\Q%s\\E][\\Q%s\\E]*", startChars.toString(), partChars.toString() );
		ClassPattern = Pattern.compile( String.format( "^(%s(\\.|/))*(%s)$", IdentifierPattern, IdentifierPattern ) );
	}
	
	public String validateClassName( String name )
	{
		if( !ClassPattern.matcher( name ).matches() )
		{
			throw new IllegalArgumentException( "Illegal name: " + name );
		}
		
		return classNameToJavaName( name );
	}
	
	public static String fileNameToClassName( String fileName )
	{
		final String suffix = ".class";
		
		if( !fileName.endsWith( suffix ) )
		{
			return null;
		}
		
		return fileName.substring( 0, fileName.length() - suffix.length() ).replace( "/", "." );
	}
	
	public static String classNameToJavaName( String className )
	{
		return className.replace( ".", "/" );
	}
}
