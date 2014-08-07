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
package cuchaz.enigma;


public class ClassFile
{
	private String m_name;
	
	public ClassFile( String name )
	{
		if( name.indexOf( '.' ) >= 0 )
		{
			throw new IllegalArgumentException( "Class name should be in JVM format!" );
		}
		m_name = name;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public String getPath( )
	{
		return m_name.replace( ".", "/" ) + ".class";
	}
	
	public boolean isInPackage( )
	{
		return m_name.indexOf( '/' ) >= 0;
	}
}
