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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ClassIndex implements Serializable
{
	private static final long serialVersionUID = -5148491146902340107L;
	
	private String m_obfName;
	private String m_deobfName;
	private BiMap<String,String> m_fieldsObfToDeobf;
	private Map<String,MethodIndex> m_methodsByObf;
	private Map<String,MethodIndex> m_methodsByDeobf;
	
	public ClassIndex( String obfName, String deobfName )
	{
		m_obfName = obfName;
		m_deobfName = deobfName;
		m_fieldsObfToDeobf = HashBiMap.create();
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

	public String getObfFieldName( String deobfName )
	{
		return m_fieldsObfToDeobf.inverse().get( deobfName );
	}
	
	public String getDeobfFieldName( String obfName )
	{
		return m_fieldsObfToDeobf.get( obfName );
	}
	
	public void setFieldName( String obfName, String deobfName )
	{
		m_fieldsObfToDeobf.put( obfName, deobfName );
	}
	
	public MethodIndex getMethodByObf( String obfName, String signature )
	{
		return m_methodsByObf.get( getMethodKey( obfName, signature ) );
	}
	
	public MethodIndex getMethodByDeobf( String deobfName, String signature )
	{
		return m_methodsByDeobf.get( getMethodKey( deobfName, signature ) );
	}
	
	private String getMethodKey( String name, String signature )
	{
		return name + signature;
	}
	
	public void setMethodNameAndSignature( String obfName, String obfSignature, String deobfName, String deobfSignature )
	{
		if( deobfName == null )
		{
			throw new IllegalArgumentException( "deobf name cannot be null!" );
		}
		
		MethodIndex methodIndex = m_methodsByObf.get( getMethodKey( obfName, obfSignature ) );
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
		for( MethodIndex methodIndex : m_methodsByObf.values() )
		{
			methodIndex.setDeobfSignature( translator.translateSignature( methodIndex.getObfSignature() ) );
		}
	}

	public void setArgumentName( String obfMethodName, String obfMethodSignature, int index, String obfName, String deobfName )
	{
		if( deobfName == null )
		{
			throw new IllegalArgumentException( "deobf name cannot be null!" );
		}
		
		MethodIndex methodIndex = m_methodsByObf.get( getMethodKey( obfMethodName, obfMethodSignature ) );
		if( methodIndex == null )
		{
			methodIndex = createMethodIndex( obfMethodName, obfMethodSignature );
		}
		methodIndex.setArgumentName( index, obfName, deobfName );
	}

	private MethodIndex createMethodIndex( String obfName, String obfSignature )
	{
		MethodIndex methodIndex = new MethodIndex( obfName, obfSignature, obfName, obfSignature );
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
		for( Map.Entry<String,String> entry : m_fieldsObfToDeobf.entrySet() )
		{
			buf.append( "\t" );
			buf.append( entry.getKey() );
			buf.append( " <-> " );
			buf.append( entry.getValue() );
			buf.append( "\n" );
		}
		buf.append( "Methods:\n" );
		for( MethodIndex methodIndex : m_methodsByObf.values() )
		{
			buf.append( methodIndex.toString() );
			buf.append( "\n" );
		}
		return buf.toString();
	}
}
