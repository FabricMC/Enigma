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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.Scanner;

import cuchaz.enigma.Util;

public class MappingsReader
{
	public Mappings read( Reader in )
	throws IOException
	{
		return read( new BufferedReader( in ) );
	}
	
	public Mappings read( BufferedReader in )
	throws IOException
	{
		Mappings mappings = new Mappings();
		ClassMapping classMapping = null;
		MethodMapping methodMapping = null;
		
		int lineNumber = 0;
		String line = null;
		while( ( line = in.readLine() ) != null )
		{
			lineNumber++;
			
			// strip comments
			int commentPos = line.indexOf( '#' );
			if( commentPos >= 0 )
			{
				line = line.substring( 0, commentPos );
			}
			
			// skip blank lines
			line = line.trim();
			if( line.length() <= 0 )
			{
				continue;
			}
			
			Scanner scanner = new Scanner( line );
			try
			{
				while( scanner.hasNext() )
				{
					// read the first token
					String token = scanner.next();
					
					if( token.equalsIgnoreCase( "CLASS" ) )
					{
						classMapping = readClass( scanner );
						mappings.addClassMapping( classMapping );
						methodMapping = null;
					}
					else if( token.equalsIgnoreCase( "FIELD" ) )
					{
						if( classMapping == null )
						{
							throw new IllegalArgumentException( "Line " + lineNumber + ": Unexpected FIELD entry here!" );
						}
						classMapping.addFieldMapping( readField( scanner ) );
					}
					else if( token.equalsIgnoreCase( "METHOD" ) )
					{
						if( classMapping == null )
						{
							throw new IllegalArgumentException( "Line " + lineNumber + ": Unexpected METHOD entry here!" );
						}
						methodMapping = readMethod( scanner );
						classMapping.addMethodMapping( methodMapping );
					}
					else if( token.equalsIgnoreCase( "ARG" ) )
					{
						if( classMapping == null || methodMapping == null )
						{
							throw new IllegalArgumentException( "Line " + lineNumber + ": Unexpected ARG entry here!" );
						}
						methodMapping.addArgumentMapping( readArgument( scanner ) );
					}
				}
			}
			catch( NoSuchElementException ex )
			{
				throw new IllegalArgumentException( "Line " + lineNumber + ": malformed line!" );
			}
			finally
			{
				Util.closeQuietly( scanner );
			}
		}
		
		return mappings;
	}

	private ArgumentMapping readArgument( Scanner scanner )
	{
		return new ArgumentMapping( scanner.nextInt(), scanner.next() );
	}

	private ClassMapping readClass( Scanner scanner )
	{
		return new ClassMapping( scanner.next(), scanner.next() );
	}
	
	private FieldMapping readField( Scanner scanner )
	{
		return new FieldMapping( scanner.next(), scanner.next() );
	}
	
	private MethodMapping readMethod( Scanner scanner )
	{
		return new MethodMapping( scanner.next(), scanner.next(), scanner.next(), scanner.next() );
	}
}
