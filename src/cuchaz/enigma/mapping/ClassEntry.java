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


public class ClassEntry implements Entry, Serializable
{
	private static final long serialVersionUID = 4235460580973955811L;
	
	private String m_name;
	
	public ClassEntry( String className )
	{
		if( className == null )
		{
			throw new IllegalArgumentException( "Class name cannot be null!" );
		}
		if( className.contains( "." ) )
		{
			throw new IllegalArgumentException( "Class name must be in JVM format. ie, path/to/package/class$inner" );
		}
		
		m_name = className;
	}
	
	public ClassEntry( ClassEntry other )
	{
		m_name = other.m_name;
	}

	@Override
	public String getName( )
	{
		return m_name;
	}
	
	@Override
	public String getClassName( )
	{
		return m_name;
	}
	
	@Override
	public int hashCode( )
	{
		return m_name.hashCode();
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof ClassEntry )
		{
			return equals( (ClassEntry)other );
		}
		return false;
	}
	
	public boolean equals( ClassEntry other )
	{
		return m_name.equals( other.m_name );
	}
	
	@Override
	public String toString( )
	{
		return m_name;
	}
}
