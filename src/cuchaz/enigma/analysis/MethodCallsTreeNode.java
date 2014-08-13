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

import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class MethodCallsTreeNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = -3658163700783307520L;
	
	private Translator m_deobfuscatingTranslator;
	private MethodEntry m_methodEntry;
	private ConstructorEntry m_constructorEntry;
	
	public MethodCallsTreeNode( Translator deobfuscatingTranslator, MethodEntry entry )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_methodEntry = entry;
		m_constructorEntry = null;
	}
	
	public MethodCallsTreeNode( Translator deobfuscatingTranslator, ConstructorEntry entry )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_methodEntry = null;
		m_constructorEntry = entry;
	}
	
	public MethodEntry getMethodEntry( )
	{
		return m_methodEntry;
	}
	
	public ConstructorEntry getConstructorEntry( )
	{
		return m_constructorEntry;
	}
	
	public Entry getEntry( )
	{
		if( m_methodEntry != null )
		{
			return m_methodEntry;
		}
		else if( m_constructorEntry != null )
		{
			return m_constructorEntry;
		}
		throw new Error( "Illegal state!" );
	}
	
	@Override
	public String toString( )
	{
		if( m_methodEntry != null )
		{
			String className = m_deobfuscatingTranslator.translateClass( m_methodEntry.getClassName() );
			if( className == null )
			{
				className = m_methodEntry.getClassName();
			}
			
			String methodName = m_deobfuscatingTranslator.translate( m_methodEntry );
			if( methodName == null )
			{
				methodName = m_methodEntry.getName();
			}
			return className + "." + methodName + "()";
		}
		else if( m_constructorEntry != null )
		{
			String className = m_deobfuscatingTranslator.translateClass( m_constructorEntry.getClassName() );
			if( className == null )
			{
				className = m_constructorEntry.getClassName();
			}
			return className + "()";
		}
		throw new Error( "Illegal state!" );
	}
	
	public void load( JarIndex index, boolean recurse )
	{
		// get all the child nodes
		for( Entry entry : index.getMethodCallers( getEntry() ) )
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
			for( Object child : children )
			{
				if( child instanceof MethodCallsTreeNode )
				{
					MethodCallsTreeNode node = (MethodCallsTreeNode)child;
					
					// don't recurse into ancestor
					Set<Entry> ancestors = Sets.newHashSet();
					TreeNode n = (TreeNode)node;
					while( n.getParent() != null )
					{
						n = n.getParent();
						if( n instanceof MethodCallsTreeNode )
						{
							ancestors.add( ((MethodCallsTreeNode)n).getEntry() );
						}
					}
					if( ancestors.contains( node.getEntry() ) )
					{
						continue;
					}
					
					node.load( index, true );
				}
			}
		}
	}
}
