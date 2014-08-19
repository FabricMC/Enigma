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

public class ConstructorEntry implements BehaviorEntry, Serializable
{
	private static final long serialVersionUID = -868346075317366758L;
	
	private ClassEntry m_classEntry;
	private String m_signature;
	
	public ConstructorEntry( ClassEntry classEntry, String signature )
	{
		if( classEntry == null )
		{
			throw new IllegalArgumentException( "Class cannot be null!" );
		}
		if( signature == null )
		{
			throw new IllegalArgumentException( "Method signature cannot be null!" );
		}
		
		m_classEntry = classEntry;
		m_signature = signature;
	}
	
	public ConstructorEntry( ConstructorEntry other )
	{
		m_classEntry = new ClassEntry( other.m_classEntry );
		m_signature = other.m_signature;
	}
	
	@Override
	public ClassEntry getClassEntry( )
	{
		return m_classEntry;
	}

	@Override
	public String getName( )
	{
		return m_classEntry.getName();
	}
	
	@Override
	public String getSignature( )
	{
		return m_signature;
	}
	
	@Override
	public String getClassName( )
	{
		return m_classEntry.getName();
	}
	
	@Override
	public int hashCode( )
	{
		return Util.combineHashesOrdered( m_classEntry, m_signature );
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof ConstructorEntry )
		{
			return equals( (ConstructorEntry)other );
		}
		return false;
	}
	
	public boolean equals( ConstructorEntry other )
	{
		return m_classEntry.equals( other.m_classEntry ) && m_signature.equals( other.m_signature );
	}
	
	@Override
	public String toString( )
	{
		return m_classEntry.getName() + m_signature;
	}
}
