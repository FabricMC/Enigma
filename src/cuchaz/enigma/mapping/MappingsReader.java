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
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.google.common.collect.Queues;

import cuchaz.enigma.Util;

public class MappingsReader
{
	public Mappings read( Reader in )
	throws IOException, MappingParseException
	{
		return read( new BufferedReader( in ) );
	}
	
	public Mappings read( BufferedReader in )
	throws IOException, MappingParseException
	{
		Mappings mappings = new Mappings();
		Deque<Object> mappingStack = Queues.newArrayDeque();
		
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
			if( line.trim().length() <= 0 )
			{
				continue;
			}
			
			// get the indent of this line
			int indent = 0;
			for( int i=0; i<line.length(); i++ )
			{
				if( line.charAt( i ) != '\t' )
				{
					break;
				}
				indent++;
			}
			
			// handle stack pops
			while( indent < mappingStack.size() )
			{
				mappingStack.pop();
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
						ClassMapping classMapping = readClass( scanner );
						if( indent == 0 )
						{
							// outer class
							mappings.addClassMapping( classMapping );
						}
						else if( indent == 1 )
						{
							// inner class
							if( !( mappingStack.getFirst() instanceof ClassMapping ) )
							{
								throw new MappingParseException( lineNumber, "Unexpected CLASS entry here!" );
							}
							((ClassMapping)mappingStack.getFirst()).addInnerClassMapping( classMapping );
						}
						else
						{
							throw new MappingParseException( lineNumber, "Unexpected CLASS entry here!" );
						}
						mappingStack.push( classMapping );
					}
					else if( token.equalsIgnoreCase( "FIELD" ) )
					{
						if( mappingStack.isEmpty() || !(mappingStack.getFirst() instanceof ClassMapping) )
						{
							throw new MappingParseException( lineNumber, "Unexpected FIELD entry here!" );
						}
						((ClassMapping)mappingStack.getFirst()).addFieldMapping( readField( scanner ) );
					}
					else if( token.equalsIgnoreCase( "METHOD" ) )
					{
						if( mappingStack.isEmpty() || !(mappingStack.getFirst() instanceof ClassMapping) )
						{
							throw new MappingParseException( lineNumber, "Unexpected METHOD entry here!" );
						}
						MethodMapping methodMapping = readMethod( scanner );
						((ClassMapping)mappingStack.getFirst()).addMethodMapping( methodMapping );
						mappingStack.push( methodMapping );
					}
					else if( token.equalsIgnoreCase( "ARG" ) )
					{
						if( mappingStack.isEmpty() || !(mappingStack.getFirst() instanceof MethodMapping) )
						{
							throw new MappingParseException( lineNumber, "Unexpected ARG entry here!" );
						}
						((MethodMapping)mappingStack.getFirst()).addArgumentMapping( readArgument( scanner ) );
					}
				}
			}
			catch( NoSuchElementException ex )
			{
				throw new MappingParseException( lineNumber, "Malformed line!" );
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
