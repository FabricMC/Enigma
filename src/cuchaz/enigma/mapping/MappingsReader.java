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

import com.google.common.collect.Queues;

import cuchaz.enigma.Constants;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

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
			
			String[] parts = line.trim().split( "\\s" );
			try
			{
				// read the first token
				String token = parts[0];
				
				if( token.equalsIgnoreCase( "CLASS" ) )
				{
					ClassMapping classMapping = readClass( parts );
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
					((ClassMapping)mappingStack.getFirst()).addFieldMapping( readField( parts ) );
				}
				else if( token.equalsIgnoreCase( "METHOD" ) )
				{
					if( mappingStack.isEmpty() || !(mappingStack.getFirst() instanceof ClassMapping) )
					{
						throw new MappingParseException( lineNumber, "Unexpected METHOD entry here!" );
					}
					MethodMapping methodMapping = readMethod( parts );
					((ClassMapping)mappingStack.getFirst()).addMethodMapping( methodMapping );
					mappingStack.push( methodMapping );
				}
				else if( token.equalsIgnoreCase( "ARG" ) )
				{
					if( mappingStack.isEmpty() || !(mappingStack.getFirst() instanceof MethodMapping) )
					{
						throw new MappingParseException( lineNumber, "Unexpected ARG entry here!" );
					}
					((MethodMapping)mappingStack.getFirst()).addArgumentMapping( readArgument( parts ) );
				}
			}
			catch( ArrayIndexOutOfBoundsException | NumberFormatException ex )
			{
				throw new MappingParseException( lineNumber, "Malformed line!" );
			}
		}
		
		return mappings;
	}

	private ArgumentMapping readArgument( String[] parts )
	{
		return new ArgumentMapping( Integer.parseInt( parts[1] ), parts[2] );
	}

	private ClassMapping readClass( String[] parts )
	{
		if( parts.length == 2 )
		{
			String obfName = parts[1];
			return new ClassMapping( moveClassOutOfDefaultPackage( obfName, Constants.NonePackage ) );
		}
		else
		{
			String obfName = parts[1];
			String deobfName = parts[2];
			if( obfName.equals( deobfName ) )
			{
				return new ClassMapping( moveClassOutOfDefaultPackage( obfName, Constants.NonePackage ) );
			}
			else
			{
				return new ClassMapping(
					moveClassOutOfDefaultPackage( parts[1], Constants.NonePackage ),
					moveClassOutOfDefaultPackage( parts[2], Constants.NonePackage )
				);
			}
		}
	}
	
	private String moveClassOutOfDefaultPackage( String className, String newPackageName )
	{
		ClassEntry classEntry = new ClassEntry( className );
		if( classEntry.isInDefaultPackage() )
		{
			return newPackageName + "/" + classEntry.getName();
		}
		return className;
	}

	private FieldMapping readField( String[] parts )
	{
		return new FieldMapping( parts[1], parts[2] );
	}
	
	private MethodMapping readMethod( String[] parts )
	{
		if( parts.length == 3 )
		{
			String obfName = parts[1];
			String obfSignature = moveSignatureOutOfDefaultPackage( parts[2], Constants.NonePackage );
			return new MethodMapping( obfName, obfSignature );
		}
		else
		{
			String obfName = parts[1];
			String deobfName = parts[2];
			String obfSignature = moveSignatureOutOfDefaultPackage( parts[3], Constants.NonePackage );
			if( obfName.equals( deobfName ) )
			{
				return new MethodMapping( obfName, obfSignature );
			}
			else
			{
				return new MethodMapping( obfName, obfSignature, deobfName );
			}
		}
	}

	private String moveSignatureOutOfDefaultPackage( String signature, final String newPackageName )
	{
		return SignatureUpdater.update( signature, new ClassNameUpdater( )
		{
			@Override
			public String update( String className )
			{
				ClassEntry classEntry = new ClassEntry( className );
				if( classEntry.isInDefaultPackage() )
				{
					return newPackageName + "/" + className;
				}
				return className;
			}
		} );
	}
}
