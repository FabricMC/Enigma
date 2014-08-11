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
package cuchaz.enigma.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cuchaz.enigma.analysis.Ancestries;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

public class Translator
{
	private TranslationDirection m_direction;
	/* TEMP */ public Map<String,ClassMapping> m_classes;
	private Ancestries m_ancestries;
	
	protected Translator( TranslationDirection direction, Map<String,ClassMapping> classes, Ancestries ancestries )
	{
		m_direction = direction;
		m_classes = classes;
		m_ancestries = ancestries;
	}
	
	public String translate( ClassEntry in )
	{
		return translateClass( in.getName() );
	}
	
	public String translateClass( String in )
	{
		ClassMapping classIndex = m_classes.get( in );
		if( classIndex != null )
		{
			return m_direction.choose(
				classIndex.getDeobfName(),
				classIndex.getObfName()
			);
		}
		
		return null;
	}
	
	public ClassEntry translateEntry( ClassEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			return in;
		}
		return new ClassEntry( name );
	}
	
	public String translate( FieldEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classIndex = m_classes.get( className );
			if( classIndex != null )
			{
				// look for the field
				String deobfName = m_direction.choose(
					classIndex.getDeobfFieldName( in.getName() ),
					classIndex.getObfFieldName( in.getName() )
				);
				if( deobfName != null )
				{
					return deobfName;
				}
			}
		}
		
		return null;
	}
	
	public FieldEntry translateEntry( FieldEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			name = in.getName();
		}
		return new FieldEntry(
			translateEntry( in.getClassEntry() ),
			name
		);
	}
	
	public String translate( MethodEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classIndex = m_classes.get( className );
			if( classIndex != null )
			{
				// look for the method
				MethodMapping methodIndex = m_direction.choose(
					classIndex.getMethodByObf( in.getName(), in.getSignature() ),
					classIndex.getMethodByDeobf( in.getName(), in.getSignature() )
				);
				if( methodIndex != null )
				{
					return m_direction.choose(
						methodIndex.getDeobfName(),
						methodIndex.getObfName()
					);
				}
			}
		}
		
		return null;
	}
	
	public MethodEntry translateEntry( MethodEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			name = in.getName();
		}
		return new MethodEntry(
			translateEntry( in.getClassEntry() ),
			name,
			translateSignature( in.getSignature() )
		);
	}
	
	public String translate( ArgumentEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classIndex = m_classes.get( className );
			if( classIndex != null )
			{
				// look for the method
				MethodMapping methodIndex = m_direction.choose(
					classIndex.getMethodByObf( in.getMethodName(), in.getMethodSignature() ),
					classIndex.getMethodByDeobf( in.getMethodName(), in.getMethodSignature() )
				);
				if( methodIndex != null )
				{
					return m_direction.choose(
						methodIndex.getDeobfArgumentName( in.getIndex() ),
						methodIndex.getObfArgumentName( in.getIndex() )
					);
				}
			}
		}
		
		return null;
	}
	
	public ArgumentEntry translateEntry( ArgumentEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			name = in.getName();
		}
		return new ArgumentEntry(
			translateEntry( in.getMethodEntry() ),
			in.getIndex(),
			name
		);
	}
	
	public String translateSignature( String signature )
	{
		return SignatureUpdater.update( signature, new ClassNameUpdater( )
		{
			@Override
			public String update( String className )
			{
				String translatedName = translateClass( className );
				if( translatedName != null )
				{
					return translatedName;
				}
				return className;
			}
		} );
	}
	
	private List<String> getSelfAndAncestors( String className )
	{
		List<String> ancestry = new ArrayList<String>();
		ancestry.add( className );
		ancestry.addAll( m_ancestries.getAncestry( className ) );
		return ancestry;
	}
}
