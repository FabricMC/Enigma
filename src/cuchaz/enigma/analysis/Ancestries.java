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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.bytecode.Descriptor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class Ancestries implements Serializable
{
	private static final long serialVersionUID = 738687982126844179L;
	
	private Map<String,String> m_superclasses;
	private Multimap<String,String> m_interfaces;
	
	public Ancestries( )
	{
		m_superclasses = Maps.newHashMap();
		m_interfaces = HashMultimap.create();
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
	
	public void addInterface( String className, String interfaceName )
	{
		className = Descriptor.toJvmName( className );
		interfaceName = Descriptor.toJvmName( interfaceName );
		
		if( className.equals( interfaceName ) )
		{
			throw new IllegalArgumentException( "Class cannot be its own interface! " + className );
		}
		
		if( !isJre( className ) && !isJre( interfaceName ) )
		{
			m_interfaces.put( className, interfaceName );
		}
	}
	
	public void renameClasses( Map<String,String> renames )
	{
		// rename superclasses
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
		
		// rename interfaces
		Set<Map.Entry<String,String>> entriesToAdd = Sets.newHashSet();
		for( Map.Entry<String,String> entry : m_interfaces.entries() )
		{
			String className = renames.get( entry.getKey() );
			if( className == null )
			{
				className = entry.getKey();
			}
			String interfaceName = renames.get( entry.getValue() );
			if( interfaceName == null )
			{
				interfaceName = entry.getValue();
			}
			entriesToAdd.add( new AbstractMap.SimpleEntry<String,String>( className, interfaceName ) );
		}
		m_interfaces.clear();
		for( Map.Entry<String,String> entry : entriesToAdd )
		{
			m_interfaces.put( entry.getKey(), entry.getValue() );
		}
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
	
	public Set<String> getInterfaces( String className )
	{
		Set<String> interfaceNames = new HashSet<String>();
		interfaceNames.addAll( m_interfaces.get( className ) );
		for( String ancestor : getAncestry( className ) )
		{
			interfaceNames.addAll( m_interfaces.get( ancestor ) );
		}
		return interfaceNames;
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
	
	public Set<String> getImplementingClasses( String targetInterfaceName )
	{
		// linear search is fast enough for now
		Set<String> classNames = Sets.newHashSet();
		for( Map.Entry<String,String> entry : m_interfaces.entries() )
		{
			String className = entry.getKey();
			String interfaceName = entry.getValue();
			if( interfaceName.equals( targetInterfaceName ) )
			{
				classNames.add( className );
				collectSubclasses( classNames, className );
			}
		}
		return classNames;
	}
	
	public boolean isInterface( String className )
	{
		return m_interfaces.containsValue( className );
	}
	
	private void collectSubclasses( Set<String> classNames, String className )
	{
		for( String subclassName : getSubclasses( className ) )
		{
			classNames.add( subclassName );
			collectSubclasses( classNames, subclassName );
		}
	}

	private boolean isJre( String className )
	{
		return className.startsWith( "java/" )
			|| className.startsWith( "javax/" );
	}
}
