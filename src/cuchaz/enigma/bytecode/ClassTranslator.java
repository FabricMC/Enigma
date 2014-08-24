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

import java.util.Map;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;

import com.beust.jcommander.internal.Maps;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class ClassTranslator
{
	private Translator m_translator;
	
	public ClassTranslator( Translator translator )
	{
		m_translator = translator;
	}
	
	public void translate( CtClass c )
	{
		// NOTE: the order of these translations is very important
		
		// translate all the field and method references in the code by editing the constant pool
		ConstPool constants = c.getClassFile().getConstPool();
		ConstPoolEditor editor = new ConstPoolEditor( constants );
		for( int i=1; i<constants.getSize(); i++ )
		{
			switch( constants.getTag( i ) )
			{
				case ConstPool.CONST_Fieldref:
				{
					// translate the name
					FieldEntry entry = new FieldEntry(
						new ClassEntry( Descriptor.toJvmName( constants.getFieldrefClassName( i ) ) ),
						constants.getFieldrefName( i )
					);
					String translatedName = m_translator.translate( entry );
					if( translatedName == null )
					{
						translatedName = entry.getName();
					}
					
					// translate the type
					String type = constants.getFieldrefType( i );
					String translatedType = m_translator.translateSignature( type );
					
					if( !entry.getName().equals( translatedName ) || !type.equals( translatedType ) )
					{
						editor.changeMemberrefNameAndType( i, translatedName, translatedType );
					}
				}
				break;
				
				case ConstPool.CONST_Methodref:
				case ConstPool.CONST_InterfaceMethodref:
				{
					// translate the name and type
					MethodEntry entry = new MethodEntry(
						new ClassEntry( Descriptor.toJvmName( editor.getMemberrefClassname( i ) ) ),
						editor.getMemberrefName( i ),
						editor.getMemberrefType( i )
					);
					String translatedName = m_translator.translate( entry );
					if( translatedName == null )
					{
						translatedName = entry.getName();
					}
					String translatedSignature = m_translator.translateSignature( entry.getSignature() );
					
					if( !entry.getName().equals( translatedName ) || !entry.getSignature().equals( translatedSignature ) )
					{
						editor.changeMemberrefNameAndType( i, translatedName, translatedSignature );
					}
				}
				break;
			}
		}
		
		ClassEntry classEntry = new ClassEntry( Descriptor.toJvmName( c.getName() ) );
		
		// translate all the fields
		for( CtField field : c.getDeclaredFields() )
		{
			// translate the name
			FieldEntry entry = new FieldEntry( classEntry, field.getName() );
			String translatedName = m_translator.translate( entry );
			if( translatedName != null )
			{
				field.setName( translatedName );
			}
			
			// translate the type
			String translatedType = m_translator.translateSignature( field.getFieldInfo().getDescriptor() );
			field.getFieldInfo().setDescriptor( translatedType );
		}
		
		// translate all the methods and constructors
		for( CtBehavior behavior : c.getDeclaredBehaviors() )
		{
			// translate the name
			MethodEntry entry = new MethodEntry( classEntry, behavior.getName(), behavior.getSignature() );
			String translatedName = m_translator.translate( entry );
			if( translatedName != null )
			{
				if( behavior instanceof CtMethod )
				{
					((CtMethod)behavior).setName( translatedName );
				}
			}

			// translate the type
			String translatedSignature = m_translator.translateSignature( behavior.getMethodInfo().getDescriptor() );
			behavior.getMethodInfo().setDescriptor( translatedSignature );
		}
		
		// translate all the class names referenced in the code
		// the above code only changed method/field/reference names and types, but not the class names themselves
		Map<ClassEntry,ClassEntry> map = Maps.newHashMap();
		for( ClassEntry obfClassEntry : ClassRenamer.getAllClassEntries( c ) )
		{
			map.put( obfClassEntry, m_translator.translateEntry( obfClassEntry ) );
		}
		ClassRenamer.renameClasses( c, map );
	}
}
