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
import java.util.Map;
import java.util.TreeMap;

public class MethodMapping implements Serializable
{
	private static final long serialVersionUID = -4409570216084263978L;
	
	private String m_obfName;
	private String m_deobfName;
	private String m_obfSignature;
	private String m_deobfSignature;
	private Map<Integer,ArgumentMapping> m_arguments;
	
	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public MethodMapping( String obfName, String deobfName, String obfSignature, String deobfSignature )
	{
		m_obfName = obfName;
		m_deobfName = deobfName;
		m_obfSignature = obfSignature;
		m_deobfSignature = deobfSignature;
		m_arguments = new TreeMap<Integer,ArgumentMapping>();
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
		m_deobfName = val;
	}
	
	public String getObfSignature( )
	{
		return m_obfSignature;
	}
	
	public String getDeobfSignature( )
	{
		return m_deobfSignature;
	}
	public void setDeobfSignature( String val )
	{
		m_deobfSignature = val;
	}
	
	public Iterable<ArgumentMapping> arguments( )
	{
		return m_arguments.values();
	}
	
	protected void addArgumentMapping( ArgumentMapping argumentMapping )
	{
		m_arguments.put( argumentMapping.getIndex(), argumentMapping );
	}
	
	public String getObfArgumentName( int index )
	{
		ArgumentMapping argumentMapping = m_arguments.get( index );
		if( argumentMapping != null )
		{
			return argumentMapping.getName();
		}
		
		return null;
	}
	
	public String getDeobfArgumentName( int index )
	{
		ArgumentMapping argumentMapping = m_arguments.get( index );
		if( argumentMapping != null )
		{
			return argumentMapping.getName();
		}
		
		return null;
	}
	
	public void setArgumentName( int index, String name )
	{
		ArgumentMapping argumentMapping = m_arguments.get( index );
		if( argumentMapping == null )
		{
			argumentMapping = new ArgumentMapping( index, name );
			m_arguments.put( index, argumentMapping );
		}
		else
		{
			argumentMapping.setName( name );
		}
	}
	
	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		buf.append( "\t" );
		buf.append( m_obfName );
		buf.append( " <-> " );
		buf.append( m_deobfName );
		buf.append( "\n" );
		buf.append( "\t" );
		buf.append( m_obfSignature );
		buf.append( " <-> " );
		buf.append( m_deobfSignature );
		buf.append( "\n" );
		buf.append( "\tArguments:\n" );
		for( ArgumentMapping argumentMapping : m_arguments.values() )
		{
			buf.append( "\t\t" );
			buf.append( argumentMapping.getIndex() );
			buf.append( " -> " );
			buf.append( argumentMapping.getName() );
			buf.append( "\n" );
		}
		return buf.toString();
	}
}
