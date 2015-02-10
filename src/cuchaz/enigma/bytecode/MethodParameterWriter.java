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
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Translator;

public class MethodParameterWriter {
	
	private Translator m_translator;
	
	public MethodParameterWriter(Translator translator) {
		m_translator = translator;
	}
	
	public void writeMethodArguments(CtClass c) {
		
		// Procyon will read method arguments from the "MethodParameters" attribute, so write those
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {
			BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);

			// get the number of arguments
			Signature signature = behaviorEntry.getSignature();
			if (signature == null) {
				// static initializers have no signatures, or arguments
				continue;
			}
			int numParams = signature.getArgumentTypes().size();
			if (numParams <= 0) {
				continue;
			}
			
			// get the list of argument names
			List<String> names = new ArrayList<String>(numParams);
			for (int i = 0; i < numParams; i++) {
				names.add(m_translator.translate(new ArgumentEntry(behaviorEntry, i, "")));
			}
			
			// save the mappings to the class
			MethodParametersAttribute.updateClass(behavior.getMethodInfo(), names);
		}
	}
}
