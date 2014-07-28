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
package cuchaz.enigma.bytecode.accessors;

public class Utf8InfoAccessor
{
	private static Class<?> m_class;
	
	static
	{
		try
		{
			m_class = Class.forName( "javassist.bytecode.Utf8Info" );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static boolean isType( ConstInfoAccessor accessor )
	{
		return m_class.isAssignableFrom( accessor.getItem().getClass() );
	}
}
