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

import java.util.regex.Pattern;

public class ClassFile
{
	private static Pattern m_obfuscatedClassPattern;
	
	static
	{
		m_obfuscatedClassPattern = Pattern.compile( "^[a-z]+$" );
	}
	
	private String m_name;
	
	public ClassFile( String name )
	{
		m_name = name;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public boolean isObfuscated( )
	{
		return m_obfuscatedClassPattern.matcher( m_name ).matches();
	}

	public String getPath( )
	{
		return m_name.replace( ".", "/" ) + ".class";
	}
}
