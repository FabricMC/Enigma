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

import java.io.IOException;
import java.util.HashMap;

import javassist.bytecode.Descriptor;

import com.google.common.collect.Maps;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

public class SourcedAst
{
	private CompilationUnitTree m_tree;
	private Trees m_trees;
	private SourcePositions m_positions;
	private HashMap<String,String> m_classNameIndex;
	
	public SourcedAst( CompilationUnitTree tree, Trees trees )
	{
		m_tree = tree;
		m_trees = trees;
		m_positions = m_trees.getSourcePositions();
		m_classNameIndex = Maps.newHashMap();
		
		// index all the class names from package imports
		for( ImportTree importTree : m_tree.getImports() )
		{
			// ignore static imports for now
			if( importTree.isStatic() )
			{
				continue;
			}
			
			// get the full and simple class names
			String fullName = Descriptor.toJvmName( importTree.getQualifiedIdentifier().toString() );
			String simpleName = fullName;
			String[] parts = fullName.split( "/" );
			if( parts.length > 0 )
			{
				simpleName = parts[parts.length - 1];
			}
			
			m_classNameIndex.put( simpleName, fullName );
		}
		
		// index the self class using the package name
		if( m_tree.getPackageName() != null )
		{
			String packageName = Descriptor.toJvmName( m_tree.getPackageName().toString() );
			for( Tree typeTree : m_tree.getTypeDecls() )
			{
				if( typeTree instanceof ClassTree )
				{
					ClassTree classTree = (ClassTree)typeTree;
					String className = classTree.getSimpleName().toString();
					m_classNameIndex.put( className, packageName + "/" + className );
				}
			}
		}
	}

	public int getStart( Tree node )
	{
		return (int)m_positions.getStartPosition( m_tree, node );
	}

	public int getEnd( Tree node )
	{
		return (int)m_positions.getEndPosition( m_tree, node );
	}
	
	public int getLine( Tree node )
	{
		return getLine( getStart( node ) );
	}
	
	public int getLine( int pos )
	{
		return (int)m_tree.getLineMap().getLineNumber( pos );
	}
	
	public CharSequence getSource( )
	{
		try
		{
			return m_tree.getSourceFile().getCharContent( true );
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
	public CharSequence getSource( Tree node )
	{
		return getSource().subSequence( getStart( node ), getEnd( node ) );
	}

	public void visit( TreeVisitor visitor )
	{
		m_tree.accept( visitor, this );
	}
	
	public String getFullClassName( String simpleClassName )
	{
		String fullClassName = m_classNameIndex.get( simpleClassName );
		if( fullClassName == null )
		{
			// no mapping was found, the name is probably already fully-qualified
			fullClassName = simpleClassName;
		}
		return fullClassName;
	}
}
