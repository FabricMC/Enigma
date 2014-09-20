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

import com.google.common.collect.Maps;

import cuchaz.enigma.analysis.TranslationIndex;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

public class Translator
{
	private TranslationDirection m_direction;
	private Map<String,ClassMapping> m_classes;
	private TranslationIndex m_index;
	
	public Translator( )
	{
		m_direction = null;
		m_classes = Maps.newHashMap();
		m_index = new TranslationIndex();
	}
	
	public Translator( TranslationDirection direction, Map<String,ClassMapping> classes, TranslationIndex index )
	{
		m_direction = direction;
		m_classes = classes;
		m_index = index;
	}
	
	@SuppressWarnings( "unchecked" )
	public <T extends Entry> T translateEntry( T entry )
	{
		if( entry instanceof ClassEntry )
		{
			return (T)translateEntry( (ClassEntry)entry );
		}
		else if( entry instanceof FieldEntry )
		{
			return (T)translateEntry( (FieldEntry)entry );
		}
		else if( entry instanceof MethodEntry )
		{
			return (T)translateEntry( (MethodEntry)entry );
		}
		else if( entry instanceof ConstructorEntry )
		{
			return (T)translateEntry( (ConstructorEntry)entry );
		}
		else if( entry instanceof ArgumentEntry )
		{
			return (T)translateEntry( (ArgumentEntry)entry );
		}
		else
		{
			throw new Error( "Unknown entry type: " + entry.getClass().getName() );
		}
	}
	
	public String translateClass( String className )
	{
		return translate( new ClassEntry( className ) );
	}
	
	public String translate( ClassEntry in )
	{
		ClassMapping classMapping = m_classes.get( in.getOuterClassName() );
		if( classMapping != null )
		{
			if( in.isInnerClass() )
			{
				// look for the inner class
				String translatedInnerClassName = m_direction.choose(
					classMapping.getDeobfInnerClassName( in.getInnerClassName() ),
					classMapping.getObfInnerClassName( in.getInnerClassName() )
				);
				if( translatedInnerClassName != null )
				{
					// return outer$inner
					String translatedOuterClassName = m_direction.choose(
						classMapping.getDeobfName(),
						classMapping.getObfName()
					);
					return translatedOuterClassName + "$" + translatedInnerClassName;
				}
			}
			else
			{
				// just return outer
				return m_direction.choose(
					classMapping.getDeobfName(),
					classMapping.getObfName()
				);
			}
		}
		return null;
	}
	
	public ClassEntry translateEntry( ClassEntry in )
	{
		String name = translate( in );
		if( name != null )
		{
			return new ClassEntry( name );
		}
		
		if( in.isInnerClass() )
		{
			// just translate the outer class name
			String outerClassName = translate( in.getOuterClassEntry() );
			if( outerClassName != null )
			{
				return new ClassEntry( outerClassName + "$" + in.getInnerClassName() );
			}
		}
		return in;
	}
	
	public String translate( FieldEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classMapping = findClassMapping( new ClassEntry( className ) );
			if( classMapping != null )
			{
				// look for the field
				String translatedName = m_direction.choose(
					classMapping.getDeobfFieldName( in.getName() ),
					classMapping.getObfFieldName( in.getName() )
				);
				if( translatedName != null )
				{
					return translatedName;
				}
			}
			
			// is the field implemented in this class?
			if( m_index.containsField( className, in.getName() ) )
			{
				// stop traversing the superclass chain
				break;
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
			// look for class
			ClassMapping classMapping = findClassMapping( new ClassEntry( className ) );
			if( classMapping != null )
			{
				// look for the method
				MethodMapping methodMapping = m_direction.choose(
					classMapping.getMethodByObf( in.getName(), in.getSignature() ),
					classMapping.getMethodByDeobf( in.getName(), translateSignature( in.getSignature() ) )
				);
				if( methodMapping != null )
				{
					return m_direction.choose(
						methodMapping.getDeobfName(),
						methodMapping.getObfName()
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
	
	public ConstructorEntry translateEntry( ConstructorEntry in )
	{
		if( in.isStatic() )
		{
			return new ConstructorEntry( translateEntry( in.getClassEntry() ) );
		}
		else
		{
			return new ConstructorEntry(
				translateEntry( in.getClassEntry() ),
				translateSignature( in.getSignature() )
			);
		}
	}
	
	public BehaviorEntry translateEntry( BehaviorEntry in )
	{
		if( in instanceof MethodEntry )
		{
			return translateEntry( (MethodEntry)in );
		}
		else if( in instanceof ConstructorEntry )
		{
			return translateEntry( (ConstructorEntry)in );
		}
		throw new Error( "Wrong entry type!" );
	}
	
	public String translate( ArgumentEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classMapping = findClassMapping( new ClassEntry( className ) );
			if( classMapping != null )
			{
				// look for the method
				MethodMapping methodMapping = m_direction.choose(
					classMapping.getMethodByObf( in.getMethodName(), in.getMethodSignature() ),
					classMapping.getMethodByDeobf( in.getMethodName(), translateSignature( in.getMethodSignature() ) )
				);
				if( methodMapping != null )
				{
					return m_direction.choose(
						methodMapping.getDeobfArgumentName( in.getIndex() ),
						methodMapping.getObfArgumentName( in.getIndex() )
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
		ancestry.addAll( m_index.getAncestry( className ) );
		return ancestry;
	}
	
	private ClassMapping findClassMapping( ClassEntry classEntry )
	{
		ClassMapping classMapping = m_classes.get( classEntry.getOuterClassName() );
		if( classMapping != null && classEntry.isInnerClass() )
		{
			classMapping = m_direction.choose(
				classMapping.getInnerClassByObf( classEntry.getInnerClassName() ),
				classMapping.getInnerClassByDeobf( classEntry.getInnerClassName() )
			);
		}
		return classMapping;
	}
}
