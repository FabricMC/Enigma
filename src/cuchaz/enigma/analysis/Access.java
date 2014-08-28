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

import java.lang.reflect.Modifier;

import javassist.CtBehavior;
import javassist.CtField;

public enum Access
{
	Public,
	Protected,
	Private;
	
	public static Access get( CtBehavior behavior )
	{
		return get( behavior.getModifiers() );
	}
	
	public static Access get( CtField field )
	{
		return get( field.getModifiers() );
	}
	
	public static Access get( int modifiers )
	{
		if( Modifier.isPublic( modifiers ) )
		{
			return Public;
		}
		else if( Modifier.isProtected( modifiers ) )
		{
			return Protected;
		}
		else if( Modifier.isPrivate( modifiers ) )
		{
			return Private;
		}
		// assume public by default
		return Public;
	}
}