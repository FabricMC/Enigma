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

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.Entry;

public class EntryReference<T extends Entry>
{
	public T entry;
	public BehaviorEntry caller;
	public int pos;
	
	public EntryReference( T entry, BehaviorEntry caller, int pos )
	{
		this.entry = entry;
		this.caller = caller;
		this.pos = pos;
	}
}
