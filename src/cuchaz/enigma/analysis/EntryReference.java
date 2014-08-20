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
package cuchaz.enigma.analysis;

import cuchaz.enigma.Util;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;

public class EntryReference<E extends Entry, C extends Entry>
{
	public E entry;
	public C context;
	public int pos;
	
	public EntryReference( E entry )
	{
		this( entry, null, -1 );
	}
	
	public EntryReference( E entry, C context, int pos )
	{
		if( entry == null )
		{
			throw new IllegalArgumentException( "Entry cannot be null!" );
		}
		
		this.entry = entry;
		this.context = context;
		this.pos = pos;
	}
	
	public ClassEntry getClassEntry( )
	{
		if( context != null )
		{
			return context.getClassEntry();
		}
		return entry.getClassEntry();
	}
	
	@Override
	public int hashCode( )
	{
		if( context != null )
		{
			return Util.combineHashesOrdered( entry.hashCode(), context.hashCode(), Integer.valueOf( pos ).hashCode() );
		}
		return entry.hashCode();
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof EntryReference )
		{
			return equals( (EntryReference<?,?>)other );
		}
		return false;
	}
	
	public boolean equals( EntryReference<?,?> other )
	{
		// check entry first
		boolean isEntrySame = entry.equals( other.entry );
		if( !isEntrySame )
		{
			return false;
		}
		
		// check caller
		if( context == null && other.context == null )
		{
			return true;
		}
		else if( context != null && other.context != null ) 
		{
			return context.equals( other.context ) && pos == other.pos;
		}
		return false;
	}
	
	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		buf.append( entry );
		if( context != null )
		{
			buf.append( " called from " );
			buf.append( context );
			buf.append( " (" );
			buf.append( pos );
			buf.append( ")" );
		}
		return buf.toString();
	}
}
