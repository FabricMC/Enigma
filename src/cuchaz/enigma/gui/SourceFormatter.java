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
package cuchaz.enigma.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class SourceFormatter
{
	public static String format( String in )
	{
		return collapseNewlines( in );
	}
	
	private static String collapseNewlines( String in )
	{
		StringBuffer buf = new StringBuffer();
		int numBlankLines = 0;
		
		BufferedReader reader = new BufferedReader( new StringReader( in ) );
		String line = null;
		try
		{
			while( ( line = reader.readLine() ) != null )
			{
				// how blank lines is this?
				boolean isBlank = line.trim().length() == 0;
				if( isBlank )
				{
					numBlankLines++;
					
					// stop printing blank lines after the first one
					if( numBlankLines < 2 )
					{
						buf.append( line );
						buf.append( "\n" );
					}
				}
				else
				{
					numBlankLines = 0;
					buf.append( line );
					buf.append( "\n" );
				}
			}
		}
		catch( IOException ex )
		{
			// StringReader will never throw an IOExecption here...
		}
		return buf.toString();
	}
}
