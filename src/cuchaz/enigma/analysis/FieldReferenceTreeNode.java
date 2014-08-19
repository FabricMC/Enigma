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

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Translator;

public class FieldReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<FieldEntry>
{
	private static final long serialVersionUID = -7934108091928699835L;
	
	private Translator m_deobfuscatingTranslator;
	private FieldEntry m_entry;
	private EntryReference<FieldEntry> m_reference;
	
	public FieldReferenceTreeNode( Translator deobfuscatingTranslator, FieldEntry entry )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
		m_reference = null;
	}
	
	private FieldReferenceTreeNode( Translator deobfuscatingTranslator, EntryReference<FieldEntry> reference )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = reference.entry;
		m_reference = reference;
	}
	
	@Override
	public FieldEntry getEntry( )
	{
		return m_entry;
	}
	
	@Override
	public EntryReference<FieldEntry> getReference( )
	{
		return m_reference;
	}
	
	@Override
	public String toString( )
	{
		if( m_reference != null )
		{
			return m_deobfuscatingTranslator.translateEntry( m_reference.caller ).toString();
		}
		return m_deobfuscatingTranslator.translateEntry( m_entry ).toString();
	}
	
	public void load( JarIndex index, boolean recurse )
	{
		// get all the child nodes
		if( m_reference == null )
		{
			for( EntryReference<FieldEntry> reference : index.getFieldReferences( m_entry ) )
			{
				add( new FieldReferenceTreeNode( m_deobfuscatingTranslator, reference ) );
			}
		}
		else
		{
			for( EntryReference<BehaviorEntry> reference : index.getBehaviorReferences( m_reference.caller ) )
			{
				add( new BehaviorReferenceTreeNode( m_deobfuscatingTranslator, reference ) );
			}
		}
		
		if( recurse && children != null )
		{
			for( Object node : children )
			{
				if( node instanceof BehaviorReferenceTreeNode )
				{
					((BehaviorReferenceTreeNode)node).load( index, true );
				}
			}
		}
	}
}
