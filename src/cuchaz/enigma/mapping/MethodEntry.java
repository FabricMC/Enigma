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

public class MethodEntry implements Serializable
{
	private static final long serialVersionUID = 4770915224467247458L;
	
	private ClassEntry m_classEntry;
	private String m_name;
	private String m_signature;
	
	public MethodEntry( ClassEntry classEntry, String name, String signature )
	{
		if( classEntry == null )
		{
			throw new IllegalArgumentException( "Class cannot be null!" );
		}
		if( name == null )
		{
			throw new IllegalArgumentException( "Method name cannot be null!" );
		}
		if( signature == null )
		{
			throw new IllegalArgumentException( "Method signature cannot be null!" );
		}
		
		m_classEntry = classEntry;
		m_name = name;
		m_signature = signature;
	}
	
	public ClassEntry getClassEntry( )
	{
		return m_classEntry;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public String getSignature( )
	{
		return m_signature;
	}
	
	@Override
	public int hashCode( )
	{
		return Util.combineHashesOrdered( m_classEntry, m_name, m_signature );
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof MethodEntry )
		{
			return equals( (MethodEntry)other );
		}
		return false;
	}
	
	public boolean equals( MethodEntry other )
	{
		return m_classEntry.equals( other.m_classEntry )
			&& m_name.equals( other.m_name )
			&& m_signature.equals( other.m_signature );
	}
	
	@Override
	public String toString( )
	{
		return m_classEntry.getName() + "." + m_name + ":" + m_signature;
	}
}
