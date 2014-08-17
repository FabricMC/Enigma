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
import javassist.bytecode.ConstPool;
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
	
	public void write( CtClass c )
	{
		// get the outer class name
		String obfClassName = Descriptor.toJvmName( c.getName() );
		String obfOuterClassName = m_jarIndex.getOuterClass( obfClassName );
		if( obfOuterClassName == null )
		{
			// this is an outer class
			obfOuterClassName = obfClassName;
		}
		else
		{
			// this is an inner class, rename it to outer$inner
			c.setName( obfOuterClassName + "$" + obfClassName );
		}
		
		// write the inner classes if needed
		Collection<String> obfInnerClassNames = m_jarIndex.getInnerClasses( obfOuterClassName );
		if( obfInnerClassNames != null )
		{
			writeInnerClasses( c, obfOuterClassName, obfInnerClassNames );
		}
	}
	
	private void writeInnerClasses( CtClass c, String obfOuterClassName, Collection<String> obfInnerClassNames )
	{
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
			String obfOuterInnerClassName = obfOuterClassName + "$" + obfInnerClassName;
			String deobfOuterInnerClassName = m_deobfuscatingTranslator.translateClass( obfOuterInnerClassName );
			if( deobfOuterInnerClassName == null )
			{
				deobfOuterInnerClassName = obfOuterInnerClassName;
			}
			String deobfInnerClassName = deobfOuterInnerClassName.substring( deobfOuterInnerClassName.lastIndexOf( '$' ) + 1 );

			// here's what the JVM spec says about the InnerClasses attribute
			// append( inner, outer of inner if inner is member of outer 0 ow, name after $ if inner not anonymous 0 ow, flags ); 
			
			// update the attribute with this inner class
			ConstPool constPool = c.getClassFile().getConstPool();
			int innerClassIndex = constPool.addClassInfo( deobfOuterInnerClassName );
			int outerClassIndex = 0;
			int innerClassSimpleNameIndex = 0;
			if( !m_jarIndex.isAnonymousClass( obfInnerClassName ) )
			{
				outerClassIndex = constPool.addClassInfo( deobfOuterClassName );
				innerClassSimpleNameIndex = constPool.addUtf8Info( deobfInnerClassName );
			}
			
			attr.append(
				innerClassIndex,
				outerClassIndex,
				innerClassSimpleNameIndex,
				c.getClassFile().getAccessFlags() & ~AccessFlag.SUPER
			);
			
			// make sure the outer class references only the new inner class names
			c.replaceClassName( obfInnerClassName, deobfOuterInnerClassName );
		}
	}
}
