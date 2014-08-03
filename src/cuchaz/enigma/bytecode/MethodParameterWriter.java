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

import java.util.ArrayList;
import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.Descriptor;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.MethodEntry;
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
		ClassEntry classEntry = new ClassEntry( Descriptor.toJvmName( c.getName() ) );
		for( CtBehavior behavior : c.getDeclaredBehaviors() )
		{
			int numParams = Descriptor.numOfParameters( behavior.getMethodInfo().getDescriptor() );
			if( numParams <= 0 )
			{
				continue;
			}
			
			// get the list of parameter names
			MethodEntry methodEntry = new MethodEntry( classEntry, behavior.getMethodInfo().getName(), behavior.getSignature() );
			List<String> names = new ArrayList<String>( numParams );
			for( int i=0; i<numParams; i++ )
			{
				names.add( m_translator.translate( new ArgumentEntry( methodEntry, i, "" ) ) );
			}
			
			// save the mappings to the class
			MethodParametersAttribute.updateClass( behavior.getMethodInfo(), names );
		}
	}
}
