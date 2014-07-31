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

public class ArgumentMapping implements Serializable
{
	private static final long serialVersionUID = 8610742471440861315L;
	
	private int m_index;
	private String m_name;
	
	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public ArgumentMapping( int index, String name )
	{
		m_index = index;
		m_name = name;
	}
	
	public int getIndex( )
	{
		return m_index;
	}
	
	public String getName( )
	{
		return m_name;
	}
	public void setName( String val )
	{
		m_name = val;
	}
}
