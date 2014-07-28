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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.beust.jcommander.internal.Maps;

import cuchaz.enigma.Util;

public class TranslationMappings implements Serializable
{
	private static final long serialVersionUID = 4649790259460259026L;
	
	private Map<String,ClassIndex> m_classesByObf;
	private Map<String,ClassIndex> m_classesByDeobf;
	private Ancestries m_ancestries;
	
	public TranslationMappings( Ancestries ancestries )
	{
		m_classesByObf = Maps.newHashMap();
		m_classesByDeobf = Maps.newHashMap();
		m_ancestries = ancestries;
	}
	
	public static TranslationMappings newFromResource( String resource )
	throws IOException
	{
		InputStream in = null;
		try
		{
			in = TranslationMappings.class.getResourceAsStream( resource );
			return newFromStream( in );
		}
		finally
		{
			Util.closeQuietly( in );
		}
	}
	
	public Translator getTranslator( TranslationDirection direction )
	{
		return new Translator(
			direction,
			direction.choose( m_classesByObf, m_classesByDeobf ),
			direction.choose( m_ancestries, new DeobfuscatedAncestries( m_ancestries, m_classesByObf, m_classesByDeobf ) )
		);
	}
	
	public void setClassName( ClassEntry obf, String deobfName )
	{
		ClassIndex classIndex = m_classesByObf.get( obf.getName() );
		if( classIndex == null )
		{
			classIndex = createClassIndex( obf );
		}
		
		m_classesByDeobf.remove( classIndex.getDeobfName() );
		classIndex.setDeobfName( deobfName );
		m_classesByDeobf.put( deobfName, classIndex );
		
		updateDeobfMethodSignatures();
		
		// TEMP
		String translatedName = getTranslator( TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void setFieldName( FieldEntry obf, String deobfName )
	{
		ClassIndex classIndex = m_classesByObf.get( obf.getClassName() );
		if( classIndex == null )
		{
			classIndex = createClassIndex( obf.getClassEntry() );
		}
		
		classIndex.setFieldName( obf.getName(), deobfName );
		
		// TEMP
		System.out.println( classIndex );
		String translatedName = getTranslator( TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void setMethodName( MethodEntry obf, String deobfName )
	{
		ClassIndex classIndex = m_classesByObf.get( obf.getClassName() );
		if( classIndex == null )
		{
			classIndex = createClassIndex( obf.getClassEntry() );
		}
		
		String deobfSignature = getTranslator( TranslationDirection.Deobfuscating ).translateSignature( obf.getSignature() );
		classIndex.setMethodNameAndSignature( obf.getName(), obf.getSignature(), deobfName, deobfSignature );
		
		// TODO: update ancestor/descendant methods in other classes in the inheritance hierarchy too
		
		// TEMP
		System.out.println( classIndex );
		String translatedName = getTranslator( TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void setArgumentName( ArgumentEntry obf, String deobfName )
	{
		ClassIndex classIndex = m_classesByObf.get( obf.getClassName() );
		if( classIndex == null )
		{
			classIndex = createClassIndex( obf.getClassEntry() );
		}
		
		classIndex.setArgumentName( obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), obf.getName(), deobfName );
		
		// TEMP
		System.out.println( classIndex );
		String translatedName = getTranslator( TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void write( OutputStream out )
	throws IOException
	{
		// TEMP: just use the object output for now. We can find a more efficient storage format later
		GZIPOutputStream gzipout = new GZIPOutputStream( out );
		ObjectOutputStream oout = new ObjectOutputStream( gzipout );
		oout.writeObject( this );
		gzipout.finish();
	}
	
	public static TranslationMappings newFromStream( InputStream in )
	throws IOException
	{
		try
		{
			return (TranslationMappings)new ObjectInputStream( new GZIPInputStream( in ) ).readObject();
		}
		catch( ClassNotFoundException ex )
		{
			throw new Error( ex );
		}
	}
	
	private ClassIndex createClassIndex( ClassEntry obf )
	{
		ClassIndex classIndex = new ClassIndex( obf.getName(), obf.getName() );
		m_classesByObf.put( classIndex.getObfName(), classIndex );
		m_classesByDeobf.put( classIndex.getDeobfName(), classIndex );
		return classIndex;
	}
	
	private void updateDeobfMethodSignatures( )
	{
		Translator translator = getTranslator( TranslationDirection.Deobfuscating );
		for( ClassIndex classIndex : m_classesByObf.values() )
		{
			classIndex.updateDeobfMethodSignatures( translator );
		}
	}
	
	@Override
	public String toString( )
	{
		StringBuilder buf = new StringBuilder();
		for( ClassIndex classIndex : m_classesByObf.values() )
		{
			buf.append( classIndex.toString() );
			buf.append( "\n" );
		}
		return buf.toString();
	}
}
