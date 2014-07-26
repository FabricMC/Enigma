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

import java.io.Closeable;
import java.io.IOException;
import java.util.jar.JarFile;


public class Util
{
	public static int combineHashesOrdered( Object ... objs )
	{
		final int prime = 67;
		int result = 1;
		for( Object obj : objs )
		{
			result *= prime;
			if( obj != null )
			{
				result += obj.hashCode();
			}
		}
		return result;
	}
	
	public static void closeQuietly( Closeable closeable )
	{
		if( closeable != null )
		{
			try
			{
				closeable.close();
			}
			catch( IOException ex )
			{
				// just ignore any further exceptions
			}
		}
	}
	
	public static void closeQuietly( JarFile jarFile )
	{
		// silly library should implement Closeable...
		if( jarFile != null )
		{
			try
			{
				jarFile.close();
			}
			catch( IOException ex )
			{
				// just ignore any further exceptions
			}
		}
	}
}
