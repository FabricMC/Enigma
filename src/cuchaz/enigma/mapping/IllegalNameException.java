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

public class IllegalNameException extends RuntimeException
{
	private static final long serialVersionUID = -2279910052561114323L;
	
	private String m_name;
	
	public IllegalNameException( String name )
	{
		m_name = name;
	}
	
	@Override
	public String getMessage( )
	{
		return "Illegal name: " + m_name;
	}
}
