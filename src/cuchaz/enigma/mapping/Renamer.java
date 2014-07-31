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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class Renamer
{
	private Ancestries m_ancestries;
	private Mappings m_mappings;
	
	public Renamer( Ancestries ancestries, Mappings mappings )
	{
		m_ancestries = ancestries;
		m_mappings = mappings;
	}
	
	public void setClassName( ClassEntry obf, String deobfName )
	{
		ClassMapping classMapping = m_mappings.m_classesByObf.get( obf.getName() );
		if( classMapping == null )
		{
			classMapping = createClassMapping( obf );
		}
		
		m_mappings.m_classesByDeobf.remove( classMapping.getDeobfName() );
		classMapping.setDeobfName( deobfName );
		m_mappings.m_classesByDeobf.put( deobfName, classMapping );
		
		updateDeobfMethodSignatures();
		
		// TEMP
		String translatedName = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void setFieldName( FieldEntry obf, String deobfName )
	{
		ClassMapping classMapping = m_mappings.m_classesByObf.get( obf.getClassName() );
		if( classMapping == null )
		{
			classMapping = createClassMapping( obf.getClassEntry() );
		}
		
		classMapping.setFieldName( obf.getName(), deobfName );
		
		// TEMP
		System.out.println( classMapping );
		String translatedName = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void setMethodName( MethodEntry obf, String deobfName )
	{
		ClassMapping classMapping = m_mappings.m_classesByObf.get( obf.getClassName() );
		if( classMapping == null )
		{
			classMapping = createClassMapping( obf.getClassEntry() );
		}
		
		String deobfSignature = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating ).translateSignature( obf.getSignature() );
		classMapping.setMethodNameAndSignature( obf.getName(), obf.getSignature(), deobfName, deobfSignature );
		
		// TODO: update ancestor/descendant methods in other classes in the inheritance hierarchy too
		
		// TEMP
		System.out.println( classMapping );
		String translatedName = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating ).translate( obf );
		assert( translatedName != null && translatedName.equals( deobfName ) );
	}
	
	public void setArgumentName( ArgumentEntry obf, String deobfName )
	{
		ClassMapping classMapping = m_mappings.m_classesByObf.get( obf.getClassName() );
		if( classMapping == null )
		{
			classMapping = createClassMapping( obf.getClassEntry() );
		}
		
		classMapping.setArgumentName( obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), deobfName );
		
		// TEMP
		System.out.println( classMapping );
		String translatedName = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating ).translate( obf );
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
	
	private ClassMapping createClassMapping( ClassEntry obf )
	{
		ClassMapping classMapping = new ClassMapping( obf.getName(), obf.getName() );
		m_mappings.m_classesByObf.put( classMapping.getObfName(), classMapping );
		m_mappings.m_classesByDeobf.put( classMapping.getDeobfName(), classMapping );
		return classMapping;
	}
	
	private void updateDeobfMethodSignatures( )
	{
		Translator translator = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating );
		for( ClassMapping classMapping : m_mappings.m_classesByObf.values() )
		{
			classMapping.updateDeobfMethodSignatures( translator );
		}
	}
}
