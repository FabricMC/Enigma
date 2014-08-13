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

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class FieldCallsTreeNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = -7934108091928699835L;
	
	private Translator m_deobfuscatingTranslator;
	private FieldEntry m_entry;
	
	public FieldCallsTreeNode( Translator deobfuscatingTranslator, FieldEntry fieldEntry )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = fieldEntry;
	}
	
	public FieldEntry getFieldEntry( )
	{
		return m_entry;
	}
	
	@Override
	public String toString( )
	{
		String className = m_deobfuscatingTranslator.translateClass( m_entry.getClassName() );
		if( className == null )
		{
			className = m_entry.getClassName();
		}
		
		String targetName = m_deobfuscatingTranslator.translate( m_entry );
		if( targetName == null )
		{
			targetName = m_entry.getName();
		}
		return className + "." + targetName;
	}
	
	public void load( JarIndex index, boolean recurse )
	{
		// get all the child nodes
		for( Entry entry : index.getFieldCallers( m_entry ) )
		{
			if( entry instanceof MethodEntry )
			{
				add( new MethodCallsTreeNode( m_deobfuscatingTranslator, (MethodEntry)entry ) );
			}
			else if( entry instanceof ConstructorEntry )
			{
				add( new MethodCallsTreeNode( m_deobfuscatingTranslator, (ConstructorEntry)entry ) );
			}
		}
		
		if( recurse && children != null )
		{
			for( Object node : children )
			{
				if( node instanceof MethodCallsTreeNode )
				{
					((MethodCallsTreeNode)node).load( index, true );
				}
			}
		}
	}
}
