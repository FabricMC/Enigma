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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javassist.bytecode.Descriptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Ancestries implements Serializable
{
	private static final long serialVersionUID = 738687982126844179L;
	
	private Map<String,String> m_superclasses;
	
	public Ancestries( )
	{
		m_superclasses = Maps.newHashMap();
	}
	
	public void addSuperclass( String className, String superclassName )
	{
		className = Descriptor.toJvmName( className );
		superclassName = Descriptor.toJvmName( superclassName );
		
		if( className.equals( superclassName ) )
		{
			throw new IllegalArgumentException( "Class cannot be its own superclass! " + className );
		}
		
		if( !isJre( className ) && !isJre( superclassName ) )
		{
			m_superclasses.put( className, superclassName );
		}
	}
	
	public void renameClasses( Map<String,String> renames )
	{
		Map<String,String> newSuperclasses = Maps.newHashMap();
		for( Map.Entry<String,String> entry : m_superclasses.entrySet() )
		{
			String subclass = renames.get( entry.getKey() );
			if( subclass == null )
			{
				subclass = entry.getKey();
			}
			String superclass = renames.get( entry.getValue() );
			if( superclass == null )
			{
				superclass = entry.getValue();
			}
			newSuperclasses.put( subclass, superclass );
		}
		m_superclasses = newSuperclasses;
	}
	
	public String getSuperclassName( String className )
	{
		return m_superclasses.get( className );
	}
	
	public List<String> getAncestry( String className )
	{
		List<String> ancestors = new ArrayList<String>();
		while( className != null )
		{
			className = getSuperclassName( className );
			if( className != null )
			{
				ancestors.add( className );
			}
		}
		return ancestors;
	}
	
	public List<String> getSubclasses( String className )
	{
		// linear search is fast enough for now
		List<String> subclasses = Lists.newArrayList();
		for( Map.Entry<String,String> entry : m_superclasses.entrySet() )
		{
			String subclass = entry.getKey();
			String superclass = entry.getValue();
			if( className.equals( superclass ) )
			{
				subclasses.add( subclass );
			}
		}
		return subclasses;
	}
	
	private boolean isJre( String className )
	{
		return className.startsWith( "java/" )
			|| className.startsWith( "javax/" );
	}
}
