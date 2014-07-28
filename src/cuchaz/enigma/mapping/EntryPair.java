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

import cuchaz.enigma.Util;

public class EntryPair
{
	public Entry obf;
	public Entry deobf;
	
	public EntryPair( Entry obf, Entry deobf )
	{
		this.obf = obf;
		this.deobf = deobf;
	}
	
	@Override
	public int hashCode( )
	{
		return Util.combineHashesOrdered( obf, deobf );
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof EntryPair )
		{
			return equals( (EntryPair)other );
		}
		return false;
	}
	
	public boolean equals( EntryPair other )
	{
		return obf.equals( other.obf ) && deobf.equals( other.deobf );
	}
}
