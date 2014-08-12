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

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import com.google.common.collect.Lists;

import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class MethodCallsTreeNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = -3658163700783307520L;
	
	private Translator m_deobfuscatingTranslator;
	private MethodEntry m_entry;
	
	public MethodCallsTreeNode( Translator deobfuscatingTranslator, MethodEntry entry )
	{
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_entry = entry;
	}
	
	public MethodEntry getMethodEntry( )
	{
		return m_entry;
	}
	
	public String getDeobfClassName( )
	{
		return m_deobfuscatingTranslator.translateClass( m_entry.getClassName() );
	}
	
	public String getDeobfMethodName( )
	{
		return m_deobfuscatingTranslator.translate( m_entry );
	}
	
	@Override
	public String toString( )
	{
		String className = getDeobfClassName();
		if( className == null )
		{
			className = m_entry.getClassName();
		}
		
		String methodName = getDeobfMethodName();
		if( methodName == null )
		{
			methodName = m_entry.getName();
		}
		return className + "." + methodName + "()";
	}
	
	public void load( JarIndex index, boolean recurse )
	{
		// get all the child nodes
		List<MethodCallsTreeNode> nodes = Lists.newArrayList();
		for( MethodEntry entry : index.getMethodCallers( m_entry ) )
		{
			nodes.add( new MethodCallsTreeNode( m_deobfuscatingTranslator, entry ) );
		}
		
		// add them to this node
		for( MethodCallsTreeNode node : nodes )
		{
			this.add( node );
		}
		
		if( recurse )
		{
			for( MethodCallsTreeNode node : nodes )
			{
				// don't recurse into self
				if( node.getMethodEntry().equals( m_entry ) )
				{
					continue;
				}
				
				node.load( index, true );
			}
		}
	}
}
