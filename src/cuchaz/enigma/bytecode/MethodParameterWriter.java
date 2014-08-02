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
package cuchaz.enigma.bytecode;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.AttributeInfo;
import cuchaz.enigma.mapping.Translator;

public class MethodParameterWriter
{
	private Translator m_translator;
	
	public MethodParameterWriter( Translator translator )
	{
		m_translator = translator;
	}
	
	public void writeMethodArguments( CtClass c )
	{
		// Procyon will read method arguments from the "MethodParameters" attribute, so write those
		for( CtBehavior behavior : c.getDeclaredBehaviors() )
		{
			AttributeInfo attribute = behavior.getMethodInfo().getAttribute( "MethodParameter" );
		}
	}
}
