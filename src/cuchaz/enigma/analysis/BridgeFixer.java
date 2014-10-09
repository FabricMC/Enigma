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

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import cuchaz.enigma.bytecode.ConstPoolEditor;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.BehaviorEntryFactory;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class BridgeFixer
{
	private JarIndex m_index;
	
	public BridgeFixer( JarIndex index )
	{
		m_index = index;
	}

	public void fixBridges( CtClass c )
	{
		// rename declared methods
		for( CtMethod method : c.getDeclaredMethods() )
		{
			// get the method entry
			MethodEntry methodEntry = new MethodEntry(
				new ClassEntry( Descriptor.toJvmName( c.getName() ) ),
				method.getName(),
				method.getSignature()
			);
			MethodEntry bridgeMethodEntry = m_index.getBridgeMethod( methodEntry );
			if( bridgeMethodEntry != null )
			{
				// fix this bridged method
				method.setName( bridgeMethodEntry.getName() );
			}
		}
		
		// rename method references
		// translate all the field and method references in the code by editing the constant pool
		ConstPool constants = c.getClassFile().getConstPool();
		ConstPoolEditor editor = new ConstPoolEditor( constants );
		for( int i=1; i<constants.getSize(); i++ )
		{
			switch( constants.getTag( i ) )
			{
				case ConstPool.CONST_Methodref:
				case ConstPool.CONST_InterfaceMethodref:
				{
					BehaviorEntry behaviorEntry = BehaviorEntryFactory.create(
						Descriptor.toJvmName( editor.getMemberrefClassname( i ) ),
						editor.getMemberrefName( i ),
						editor.getMemberrefType( i )
					);
					
					if( behaviorEntry instanceof MethodEntry )
					{
						MethodEntry methodEntry = (MethodEntry)behaviorEntry;
						
						// translate the name and type
						MethodEntry bridgeMethodEntry = m_index.getBridgeMethod( methodEntry );
						if( bridgeMethodEntry != null )
						{
							// FIXIT FIXIT FIXIT FIXIT FIXIT FIXIT FIXIT
							editor.changeMemberrefNameAndType( i, bridgeMethodEntry.getName(), bridgeMethodEntry.getSignature() );
						}
					}
				}
				break;
			}
		}
	}
}
