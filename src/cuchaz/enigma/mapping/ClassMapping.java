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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Maps;

public class ClassMapping implements Serializable, Comparable<ClassMapping>
{
	private static final long serialVersionUID = -5148491146902340107L;
	
	private String m_obfName;
	private String m_deobfName;
	private Map<String,ClassMapping> m_innerClassesByObf;
	private Map<String,ClassMapping> m_innerClassesByDeobf;
	private Map<String,FieldMapping> m_fieldsByObf;
	private Map<String,FieldMapping> m_fieldsByDeobf;
	private Map<String,MethodMapping> m_methodsByObf;
	private Map<String,MethodMapping> m_methodsByDeobf;
	
	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public ClassMapping( String obfName, String deobfName )
	{
		m_obfName = obfName;
		m_deobfName = NameValidator.validateClassName( deobfName );
		m_innerClassesByObf = Maps.newHashMap();
		m_innerClassesByDeobf = Maps.newHashMap();
		m_fieldsByObf = Maps.newHashMap();
		m_fieldsByDeobf = Maps.newHashMap();
		m_methodsByObf = Maps.newHashMap();
		m_methodsByDeobf = Maps.newHashMap();
	}

	public String getObfName( )
	{
		return m_obfName;
	}
	
	public String getDeobfName( )
	{
		return m_deobfName;
	}
	public void setDeobfName( String val )
	{
		m_deobfName = NameValidator.validateClassName( val );
	}
	
	//// INNER CLASSES ////////
	
	public Iterable<ClassMapping> innerClasses( )
	{
		assert( m_innerClassesByObf.size() == m_innerClassesByDeobf.size() );
		return m_innerClassesByObf.values();
	}
	
	protected void addInnerClassMapping( ClassMapping classMapping )
	{
		m_innerClassesByObf.put( classMapping.getObfName(), classMapping );
		m_innerClassesByDeobf.put( classMapping.getDeobfName(), classMapping );
	}
	
	public ClassMapping getOrCreateInnerClass( String obfName )
	{
		ClassMapping classMapping = m_innerClassesByObf.get( obfName );
		if( classMapping == null )
		{
			classMapping = new ClassMapping( obfName, obfName );
			m_innerClassesByObf.put( obfName, classMapping );
			m_innerClassesByDeobf.put( obfName, classMapping );
		}
		return classMapping;
	}
	
	public ClassMapping getInnerClassByObf( String obfName )
	{
		return m_innerClassesByObf.get( obfName );
	}
	
	public ClassMapping getInnerClassByDeobf( String deobfName )
	{
		return m_innerClassesByDeobf.get( deobfName );
	}
	
	public String getObfInnerClassName( String deobfName )
	{
		ClassMapping classMapping = m_innerClassesByDeobf.get( deobfName );
		if( classMapping != null )
		{
			return classMapping.getObfName();
		}
		return null;
	}
	
	public String getDeobfInnerClassName( String obfName )
	{
		ClassMapping classMapping = m_innerClassesByObf.get( obfName );
		if( classMapping != null )
		{
			return classMapping.getDeobfName();
		}
		return null;
	}
	
	public void setInnerClassName( String obfName, String deobfName )
	{
		ClassMapping classMapping = getOrCreateInnerClass( obfName );
		m_innerClassesByDeobf.remove( classMapping.getDeobfName() );
		classMapping.setDeobfName( deobfName );
		m_innerClassesByDeobf.put( deobfName, classMapping );
	}
	
	//// FIELDS ////////
	
	public Iterable<FieldMapping> fields( )
	{
		assert( m_fieldsByObf.size() == m_fieldsByDeobf.size() );
		return m_fieldsByObf.values();
	}
	
	protected void addFieldMapping( FieldMapping fieldMapping )
	{
		if( m_fieldsByObf.containsKey( fieldMapping.getObfName() ) )
		{
			throw new Error( "Already have mapping for " + m_obfName + "." + fieldMapping.getObfName() );
		}
		if( m_fieldsByDeobf.containsKey( fieldMapping.getDeobfName() ) )
		{
			throw new Error( "Already have mapping for " + m_deobfName + "." + fieldMapping.getDeobfName() );
		}
		m_fieldsByObf.put( fieldMapping.getObfName(), fieldMapping );
		m_fieldsByDeobf.put( fieldMapping.getDeobfName(), fieldMapping );
		assert( m_fieldsByObf.size() == m_fieldsByDeobf.size() );
	}

	public String getObfFieldName( String deobfName )
	{
		FieldMapping fieldMapping = m_fieldsByDeobf.get( deobfName );
		if( fieldMapping != null )
		{
			return fieldMapping.getObfName();
		}
		return null;
	}
	
	public String getDeobfFieldName( String obfName )
	{
		FieldMapping fieldMapping = m_fieldsByObf.get( obfName );
		if( fieldMapping != null )
		{
			return fieldMapping.getDeobfName();
		}
		return null;
	}
	
	public void setFieldName( String obfName, String deobfName )
	{
		FieldMapping fieldMapping = m_fieldsByObf.get( obfName );
		if( fieldMapping == null )
		{
			fieldMapping = new FieldMapping( obfName, deobfName );
			m_fieldsByObf.put( obfName, fieldMapping );
			m_fieldsByDeobf.put( deobfName, fieldMapping );
		}
		
		m_fieldsByDeobf.remove( fieldMapping.getDeobfName() );
		fieldMapping.setDeobfName( deobfName );
		m_fieldsByDeobf.put( deobfName, fieldMapping );
	}
	
	//// METHODS ////////
	
	public Iterable<MethodMapping> methods( )
	{
		assert( m_methodsByObf.size() == m_methodsByDeobf.size() );
		return m_methodsByObf.values();
	}
	
	protected void addMethodMapping( MethodMapping methodMapping )
	{
		String obfKey = getMethodKey( methodMapping.getObfName(), methodMapping.getObfSignature() );
		String deobfKey = getMethodKey( methodMapping.getDeobfName(), methodMapping.getDeobfSignature() );
		if( m_methodsByObf.containsKey( obfKey ) )
		{
			throw new Error( "Already have mapping for " + m_obfName + "." + obfKey );
		}
		if( m_methodsByDeobf.containsKey( deobfKey ) )
		{
			throw new Error( "Already have mapping for " + m_deobfName + "." + deobfKey );
		}
		m_methodsByObf.put( obfKey, methodMapping );
		m_methodsByDeobf.put( deobfKey, methodMapping );
		assert( m_methodsByObf.size() == m_methodsByDeobf.size() );
	}
	
	public MethodMapping getMethodByObf( String obfName, String signature )
	{
		return m_methodsByObf.get( getMethodKey( obfName, signature ) );
	}
	
	public MethodMapping getMethodByDeobf( String deobfName, String signature )
	{
		return m_methodsByDeobf.get( getMethodKey( deobfName, signature ) );
	}
	
	private String getMethodKey( String name, String signature )
	{
		if( name == null )
		{
			throw new IllegalArgumentException( "name cannot be null!" );
		}
		if( signature == null )
		{
			throw new IllegalArgumentException( "signature cannot be null!" );
		}
		return name + signature;
	}
	
	public void setMethodNameAndSignature( String obfName, String obfSignature, String deobfName, String deobfSignature )
	{
		MethodMapping methodMapping = m_methodsByObf.get( getMethodKey( obfName, obfSignature ) );
		if( methodMapping == null )
		{
			methodMapping = createMethodIndex( obfName, obfSignature );
		}
		
		m_methodsByDeobf.remove( getMethodKey( methodMapping.getDeobfName(), methodMapping.getDeobfSignature() ) );
		methodMapping.setDeobfName( deobfName );
		methodMapping.setDeobfSignature( deobfSignature );
		m_methodsByDeobf.put( getMethodKey( deobfName, deobfSignature ), methodMapping );
	}
	
	public void updateDeobfMethodSignatures( Translator translator )
	{
		for( MethodMapping methodIndex : m_methodsByObf.values() )
		{
			methodIndex.setDeobfSignature( translator.translateSignature( methodIndex.getObfSignature() ) );
		}
	}

	//// ARGUMENTS ////////
	
	public void setArgumentName( String obfMethodName, String obfMethodSignature, int argumentIndex, String argumentName )
	{
		MethodMapping methodIndex = m_methodsByObf.get( getMethodKey( obfMethodName, obfMethodSignature ) );
		if( methodIndex == null )
		{
			methodIndex = createMethodIndex( obfMethodName, obfMethodSignature );
		}
		methodIndex.setArgumentName( argumentIndex, argumentName );
	}

	private MethodMapping createMethodIndex( String obfName, String obfSignature )
	{
		MethodMapping methodMapping = new MethodMapping( obfName, obfName, obfSignature, obfSignature );
		String key = getMethodKey( obfName, obfSignature );
		m_methodsByObf.put( key, methodMapping );
		m_methodsByDeobf.put( key, methodMapping );
		return methodMapping;
	}

	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		buf.append( m_obfName );
		buf.append( " <-> " );
		buf.append( m_deobfName );
		buf.append( "\n" );
		buf.append( "Fields:\n" );
		for( FieldMapping fieldMapping : fields() )
		{
			buf.append( "\t" );
			buf.append( fieldMapping.getObfName() );
			buf.append( " <-> " );
			buf.append( fieldMapping.getDeobfName() );
			buf.append( "\n" );
		}
		buf.append( "Methods:\n" );
		for( MethodMapping methodIndex : m_methodsByObf.values() )
		{
			buf.append( methodIndex.toString() );
			buf.append( "\n" );
		}
		return buf.toString();
	}
	
	@Override
	public int compareTo( ClassMapping other )
	{
		return m_obfName.compareTo( other.m_obfName );
	}
	
	public void renameObfClasses( Map<String,String> nameMap )
	{
		// rename self
		{
			String newName = nameMap.get( m_obfName );
			if( newName != null )
			{
				m_obfName = newName;
			}
		}
		
		// rename inner classes
		for( ClassMapping classMapping : new ArrayList<ClassMapping>( m_innerClassesByObf.values() ) )
		{
			m_innerClassesByObf.remove( classMapping.getObfName() );
			classMapping.renameObfClasses( nameMap );
			m_innerClassesByObf.put( classMapping.getObfName(), classMapping );
		}
		
		// rename method signatures
		for( MethodMapping methodMapping : new ArrayList<MethodMapping>( m_methodsByObf.values() ) )
		{
			m_methodsByObf.remove( getMethodKey( methodMapping.getObfName(), methodMapping.getObfSignature() ) );
			methodMapping.renameObfClasses( nameMap );
			m_methodsByObf.put( getMethodKey( methodMapping.getObfName(), methodMapping.getObfSignature() ), methodMapping );
		}
	}
}
