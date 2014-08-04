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

public class FieldMapping implements Serializable, Comparable<FieldMapping>
{
	private static final long serialVersionUID = 8610742471440861315L;
	
	private String m_obfName;
	private String m_deobfName;
	
	public FieldMapping( String obfName, String deobfName )
	{
		m_obfName = obfName;
		m_deobfName = NameValidator.validateFieldName( deobfName );
	}

	public String getObfName( )
	{
		return m_obfName;
	}

	public String getDeobfName( )
	{
		return m_deobfName;
	}
	public void setDeobfName( String val )
	{
		m_deobfName = NameValidator.validateFieldName( val );
	}
	
	@Override
	public int compareTo( FieldMapping other )
	{
		return m_obfName.compareTo( other.m_obfName );
	}
}
