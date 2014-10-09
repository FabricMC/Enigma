/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.bytecode.Descriptor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class TranslationIndex implements Serializable
{
	private static final long serialVersionUID = 738687982126844179L;
	
	private Map<String,String> m_superclasses;
	private Multimap<String,String> m_fields;
	
	public TranslationIndex( )
	{
		m_superclasses = Maps.newHashMap();
		m_fields = HashMultimap.create();
	}
	
	public TranslationIndex( TranslationIndex other )
	{
		m_superclasses = Maps.newHashMap( other.m_superclasses );
		m_fields = HashMultimap.create( other.m_fields );
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
	
	public void addField( String className, String fieldName )
	{
		m_fields.put( className, fieldName );
	}
	
	public void renameClasses( Map<String,String> renames )
	{
		EntryRenamer.renameClassesInMap( renames, m_superclasses );
		EntryRenamer.renameClassesInMultimap( renames, m_fields );
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
	
	public List<String> getSubclassNames( String className )
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
	
	public void getSubclassNamesRecursively( Set<String> out, String className )
	{
		for( String subclassName : getSubclassNames( className ) )
		{
			out.add( subclassName );
			getSubclassNamesRecursively( out, subclassName );
		}
	}
	
	public boolean containsField( String className, String fieldName )
	{
		return m_fields.containsEntry( className, fieldName );
	}
	
	private boolean isJre( String className )
	{
		return className.startsWith( "java/" )
			|| className.startsWith( "javax/" );
	}
}
