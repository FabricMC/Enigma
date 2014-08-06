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
package cuchaz.enigma.gui;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import com.beust.jcommander.internal.Lists;

import cuchaz.enigma.mapping.Ancestries;

public class ClassInheritanceTreeNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = 4432367405826178490L;
	
	String m_className;
	
	public ClassInheritanceTreeNode( String className )
	{
		m_className = className;
	}
	
	public String getClassName( )
	{
		return m_className;
	}
	
	@Override
	public String toString( )
	{
		return m_className;
	}
	
	public void load( Ancestries ancestries, boolean recurse )
	{
		// get all the child nodes
		List<ClassInheritanceTreeNode> nodes = Lists.newArrayList();
		for( String subclassName : ancestries.getSubclasses( m_className ) )
		{
			nodes.add( new ClassInheritanceTreeNode( subclassName ) );
		}
		
		// add then to this node
		for( ClassInheritanceTreeNode node : nodes )
		{
			this.add( node );
		}
		
		if( recurse )
		{
			for( ClassInheritanceTreeNode node : nodes )
			{
				node.load( ancestries, true );
			}
		}
	}
}
