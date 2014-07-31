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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class MappingsWriter
{
	public void write( Writer out, Mappings mappings )
	throws IOException
	{
		write( new PrintWriter( out ), mappings );
	}
	
	public void write( PrintWriter out, Mappings mappings )
	throws IOException
	{
		for( ClassMapping classMapping : mappings.classes() )
		{
			write( out, classMapping );
		}
	}
	
	public void write( PrintWriter out, ClassMapping classMapping )
	throws IOException
	{
		out.format( "CLASS %s %s\n", classMapping.getObfName(), classMapping.getDeobfName() );
		
		for( FieldMapping fieldMapping : classMapping.fields() )
		{
			write( out, fieldMapping );
		}
		
		for( MethodMapping methodMapping : classMapping.methods() )
		{
			write( out, methodMapping );
		}
	}

	public void write( PrintWriter out, FieldMapping fieldMapping )
	throws IOException
	{
		out.format( "\tFIELD %s %s\n", fieldMapping.getObfName(), fieldMapping.getDeobfName() );
	}
	
	public void write( PrintWriter out, MethodMapping methodMapping )
	throws IOException
	{
		out.format( "\tMETHOD %s %s %s %s\n",
			methodMapping.getObfName(), methodMapping.getDeobfName(),
			methodMapping.getObfSignature(), methodMapping.getDeobfSignature()
		);
		
		for( ArgumentMapping argumentMapping : methodMapping.arguments() )
		{
			write( out, argumentMapping );
		}
	}

	public void write( PrintWriter out, ArgumentMapping argumentMapping )
	throws IOException
	{
		out.format( "\t\tARG %d %s\n", argumentMapping.getIndex(), argumentMapping.getName() );
	}
}
