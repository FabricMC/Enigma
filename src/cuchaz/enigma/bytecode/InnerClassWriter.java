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

import java.util.Collection;

import javassist.CtClass;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;
import javassist.bytecode.InnerClassesAttribute;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.Translator;

public class InnerClassWriter
{
	private Translator m_deobfuscatingTranslator;
	private JarIndex m_jarIndex;
	
	public InnerClassWriter( Translator deobfuscatingTranslator, JarIndex jarIndex )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_jarIndex = jarIndex;
	}

	public void writeInnerClasses( CtClass c )
	{
		// is this an outer class with inner classes?
		String obfOuterClassName = Descriptor.toJvmName( c.getName() );
		Collection<String> obfInnerClassNames = m_jarIndex.getInnerClasses( obfOuterClassName );
		if( obfInnerClassNames != null && !obfInnerClassNames.isEmpty() )
		{
			writeInnerClasses( c, obfInnerClassNames );
		}
	}

	private void writeInnerClasses( CtClass c, Collection<String> obfInnerClassNames )
	{
		String obfOuterClassName = Descriptor.toJvmName( c.getName() );
		InnerClassesAttribute attr = new InnerClassesAttribute( c.getClassFile().getConstPool() );
		c.getClassFile().addAttribute( attr );
		for( String obfInnerClassName : obfInnerClassNames )
		{
			// deobfuscate the class names
			String deobfOuterClassName = m_deobfuscatingTranslator.translateClass( obfOuterClassName );
			if( deobfOuterClassName == null )
			{
				deobfOuterClassName = obfOuterClassName;
			}
			String deobfInnerClassName = m_deobfuscatingTranslator.translateClass( obfInnerClassName );
			if( deobfInnerClassName == null )
			{
				deobfInnerClassName = obfInnerClassName;
			}
			
			// update the attribute
			String deobfOuterInnerClassName = deobfOuterClassName + "$" + deobfInnerClassName;
			attr.append(
					deobfOuterInnerClassName,
				deobfOuterClassName,
				deobfInnerClassName,
				c.getClassFile().getAccessFlags() & ~AccessFlag.SUPER
			);
			
			// make sure the outer class references only the new inner class names
			c.replaceClassName( obfInnerClassName, deobfOuterInnerClassName );
			
			// TEMP
			System.out.println( "\tInner " + obfInnerClassName + " -> " + deobfOuterInnerClassName );
		}
	}
}
