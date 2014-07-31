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
import java.util.Map;

import com.beust.jcommander.internal.Maps;

public class ClassMapping implements Serializable
{
	private static final long serialVersionUID = -5148491146902340107L;
	
	private String m_obfName;
	private String m_deobfName;
	private Map<String,FieldMapping> m_fieldsByObf;
	private Map<String,FieldMapping> m_fieldsByDeobf;
	private Map<String,MethodMapping> m_methodsByObf;
	private Map<String,MethodMapping> m_methodsByDeobf;
	
	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public ClassMapping( String obfName, String deobfName )
	{
		m_obfName = obfName;
		m_deobfName = deobfName;
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
		m_deobfName = val;
	}
	
	public Iterable<FieldMapping> fields( )
	{
		assert( m_fieldsByObf.size() == m_fieldsByDeobf.size() );
		return m_fieldsByObf.values();
	}
	
	protected void addFieldMapping( FieldMapping fieldMapping )
	{
		m_fieldsByObf.put( fieldMapping.getObfName(), fieldMapping );
		m_fieldsByDeobf.put( fieldMapping.getDeobfName(), fieldMapping );
	}

	public Iterable<MethodMapping> methods( )
	{
		assert( m_methodsByObf.size() == m_methodsByDeobf.size() );
		return m_methodsByObf.values();
	}
	
	protected void addMethodMapping( MethodMapping methodMapping )
	{
		m_methodsByObf.put( getMethodKey( methodMapping.getObfName(), methodMapping.getObfSignature() ), methodMapping );
		m_methodsByDeobf.put( getMethodKey( methodMapping.getDeobfName(), methodMapping.getDeobfSignature() ), methodMapping );
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
		if( deobfName == null )
		{
			throw new IllegalArgumentException( "deobf name cannot be null!" );
		}
		
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
		if( deobfName == null )
		{
			throw new IllegalArgumentException( "deobf name cannot be null!" );
		}
		
		MethodMapping methodIndex = m_methodsByObf.get( getMethodKey( obfName, obfSignature ) );
		if( methodIndex == null )
		{
			methodIndex = createMethodIndex( obfName, obfSignature );
		}
		
		m_methodsByDeobf.remove( getMethodKey( methodIndex.getDeobfName(), methodIndex.getDeobfSignature() ) );
		methodIndex.setDeobfName( deobfName );
		methodIndex.setDeobfSignature( deobfSignature );
		m_methodsByDeobf.put( getMethodKey( deobfName, deobfSignature ), methodIndex );
	}
	
	public void updateDeobfMethodSignatures( Translator translator )
	{
		for( MethodMapping methodIndex : m_methodsByObf.values() )
		{
			methodIndex.setDeobfSignature( translator.translateSignature( methodIndex.getObfSignature() ) );
		}
	}

	public void setArgumentName( String obfMethodName, String obfMethodSignature, int argumentIndex, String argumentName )
	{
		if( argumentName == null )
		{
			throw new IllegalArgumentException( "argument name cannot be null!" );
		}
		
		MethodMapping methodIndex = m_methodsByObf.get( getMethodKey( obfMethodName, obfMethodSignature ) );
		if( methodIndex == null )
		{
			methodIndex = createMethodIndex( obfMethodName, obfMethodSignature );
		}
		methodIndex.setArgumentName( argumentIndex, argumentName );
	}

	private MethodMapping createMethodIndex( String obfName, String obfSignature )
	{
		MethodMapping methodIndex = new MethodMapping( obfName, obfName, obfSignature, obfSignature );
		String key = getMethodKey( obfName, obfSignature );
		m_methodsByObf.put( key, methodIndex );
		m_methodsByDeobf.put( key, methodIndex );
		return methodIndex;
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
}
