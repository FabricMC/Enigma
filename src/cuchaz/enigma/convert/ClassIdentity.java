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
package cuchaz.enigma.convert;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;

import cuchaz.enigma.Util;
import cuchaz.enigma.bytecode.ConstPoolEditor;
import cuchaz.enigma.bytecode.InfoType;
import cuchaz.enigma.bytecode.accessors.ConstInfoAccessor;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.SignatureUpdater;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

public class ClassIdentity
{
	private ClassEntry m_classEntry;
	private Set<String> m_fields;
	private Set<String> m_methods;
	private Set<String> m_constructors;
	private String m_staticInitializer;
	
	public ClassIdentity( CtClass c )
	{
		m_classEntry = new ClassEntry( Descriptor.toJvmName( c.getName() ) );
		m_fields = Sets.newHashSet();
		for( CtField field : c.getDeclaredFields() )
		{
			m_fields.add( scrubSignature( scrubSignature( field.getSignature() ) ) );
		}
		m_methods = Sets.newHashSet();
		for( CtMethod method : c.getDeclaredMethods() )
		{
			m_methods.add( scrubSignature( method.getSignature() ) + "0x" + getBehaviorSignature( method ) );
		}
		m_constructors = Sets.newHashSet();
		for( CtConstructor constructor : c.getDeclaredConstructors() )
		{
			m_constructors.add( scrubSignature( constructor.getSignature() ) + "0x" + getBehaviorSignature( constructor ) );
		}
		m_staticInitializer = "";
		if( c.getClassInitializer() != null )
		{
			m_staticInitializer = getBehaviorSignature( c.getClassInitializer() );
		}
	}

	public ClassEntry getClassEntry( )
	{
		return m_classEntry;
	}
	
	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		buf.append( "class: " );
		buf.append( hashCode() );
		buf.append( "\n" );
		for( String field : m_fields )
		{
			buf.append( "\tfield " );
			buf.append( field );
			buf.append( "\n" );
		}
		for( String method : m_methods )
		{
			buf.append( "\tmethod " );
			buf.append( method );
			buf.append( "\n" );
		}
		for( String constructor : m_constructors )
		{
			buf.append( "\tconstructor " );
			buf.append( constructor );
			buf.append( "\n" );
		}
		if( m_staticInitializer.length() > 0 )
		{
			buf.append( "\tinitializer " );
			buf.append( m_staticInitializer );
			buf.append( "\n" );
		}
		return buf.toString();
	}
	
	private String scrubSignature( String signature )
	{
		return SignatureUpdater.update( signature, new ClassNameUpdater( )
		{
			private Map<String,String> m_classNames = Maps.newHashMap();
			
			@Override
			public String update( String className )
			{
				// does the class have a package?
				if( className.indexOf( '/' ) >= 0 )
				{
					return className;
				}
				
				if( !m_classNames.containsKey( className ) )
				{
					m_classNames.put( className, getNewClassName() );
				}
				return m_classNames.get( className );
			}

			private String getNewClassName( )
			{
				return String.format( "C%03d", m_classNames.size() );
			}
		} );
	}
	
	private String getBehaviorSignature( CtBehavior behavior )
	{
		try
		{
			// does this method have an implementation?
			if( behavior.getMethodInfo().getCodeAttribute() == null )
			{
				return "(none)";
			}
			
			// compute the hash from the opcodes
			ConstPool constants = behavior.getMethodInfo().getConstPool();
			ConstPoolEditor editor = new ConstPoolEditor( constants );
			MessageDigest digest = MessageDigest.getInstance( "MD5" );
			CodeIterator iter = behavior.getMethodInfo().getCodeAttribute().iterator();
			while( iter.hasNext() )
			{
				int pos = iter.next();
				
				// update the hash with the opcode
				int opcode = iter.byteAt( pos );
				digest.update( (byte)opcode );
				
				// is there a constant value here?
				int constIndex = -1;
				switch( opcode )
				{
					case Opcode.LDC:
						constIndex = iter.byteAt( pos + 1 );
					break;
					
					case Opcode.LDC_W:
						constIndex = ( iter.byteAt( pos + 1 ) << 8 ) | iter.byteAt( pos + 2 );
					break;
					
					case Opcode.LDC2_W:
						constIndex = ( iter.byteAt( pos + 1 ) << 8 ) | iter.byteAt( pos + 2 );
					break;
				}
				
				if( constIndex >= 0 )
				{
					// update the hash with the constant value
					ConstInfoAccessor item = editor.getItem( constIndex );
					if( item.getType() == InfoType.StringInfo )
					{
						String val = constants.getStringInfo( constIndex );
						try
						{
							digest.update( val.getBytes( "UTF8" ) );
						}
						catch( UnsupportedEncodingException ex )
						{
							throw new Error( ex );
						}
					}
				}
			}
			
			// convert the hash to a hex string
			return toHex( digest.digest() );
		}
		catch( BadBytecode | NoSuchAlgorithmException ex )
		{
			throw new Error( ex );
		}
	}
	
	private String toHex( byte[] bytes )
	{
		// function taken from:
		// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for( int j = 0; j < bytes.length; j++ )
		{
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String( hexChars );
	}

	@Override
	public boolean equals( Object other )
	{
		if( other instanceof ClassIdentity )
		{
			return equals( (ClassIdentity)other );
		}
		return false;
	}
	
	public boolean equals( ClassIdentity other )
	{
		return m_fields.equals( other.m_fields )
			&& m_methods.equals( other.m_methods )
			&& m_constructors.equals( other.m_constructors )
			&& m_staticInitializer.equals( other.m_staticInitializer );
	}
	
	@Override
	public int hashCode( )
	{
		List<Object> objs = Lists.newArrayList();
		objs.addAll( m_fields );
		objs.addAll( m_methods );
		objs.addAll( m_constructors );
		objs.add( m_staticInitializer );
		return Util.combineHashesOrdered( objs );
	}
}
