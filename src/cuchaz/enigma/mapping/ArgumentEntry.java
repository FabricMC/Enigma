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

import java.io.Serializable;

import cuchaz.enigma.Util;

public class ArgumentEntry implements Serializable
{
	private static final long serialVersionUID = 4472172468162696006L;
	
	private MethodEntry m_methodEntry;
	private int m_index;
	private String m_name;
	
	public ArgumentEntry( MethodEntry methodEntry, int index, String name )
	{
		if( methodEntry == null )
		{
			throw new IllegalArgumentException( "Method cannot be null!" );
		}
		if( index < 0 )
		{
			throw new IllegalArgumentException( "Index must be non-negative!" );
		}
		if( name == null )
		{
			throw new IllegalArgumentException( "Argument name cannot be null!" );
		}
		
		m_methodEntry = methodEntry;
		m_index = index;
		m_name = name;
	}
	
	public MethodEntry getMethodEntry( )
	{
		return m_methodEntry;
	}
	
	public int getIndex( )
	{
		return m_index;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	@Override
	public int hashCode( )
	{
		return Util.combineHashesOrdered( m_methodEntry, Integer.valueOf( m_index ).hashCode(), m_name.hashCode() );
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof ArgumentEntry )
		{
			return equals( (ArgumentEntry)other );
		}
		return false;
	}
	
	public boolean equals( ArgumentEntry other )
	{
		return m_methodEntry.equals( other.m_methodEntry )
			&& m_index == other.m_index
			&& m_name.equals( other.m_name );
	}
	
	@Override
	public String toString( )
	{
		return m_methodEntry.toString() + "(" + m_index + ":" + m_name + ")";
	}
}
