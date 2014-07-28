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
package cuchaz.enigma;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import cuchaz.enigma.mapping.Ancestries;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.TranslationDirection;
import cuchaz.enigma.mapping.TranslationMappings;
import cuchaz.enigma.mapping.Translator;

public class Deobfuscator
{
	private File m_file;
	private JarFile m_jar;
	private DecompilerSettings m_settings;
	private Ancestries m_ancestries;
	private TranslationMappings m_mappings;
	
	private static Comparator<ClassFile> m_obfuscatedClassSorter;
	
	static
	{
		m_obfuscatedClassSorter = new Comparator<ClassFile>( )
		{
			@Override
			public int compare( ClassFile a, ClassFile b )
			{
				if( a.getName().length() != b.getName().length() )
				{
					return a.getName().length() - b.getName().length();
				}
				
				return a.getName().compareTo( b.getName() );
			}
		};
	}
	
	public Deobfuscator( File file )
	throws IOException
	{
		m_file = file;
		m_jar = new JarFile( m_file );
		
		// build the ancestries
		InputStream jarIn = null;
		try
		{
			m_ancestries = new Ancestries();
			jarIn = new FileInputStream( m_file );
			m_ancestries.readFromJar( jarIn );
		}
		finally
		{
			Util.closeQuietly( jarIn );
		}
		
		// init mappings
		m_mappings = new TranslationMappings( m_ancestries );
		
		// config the decompiler
		m_settings = DecompilerSettings.javaDefaults();
		m_settings.setTypeLoader( new TranslatingTypeLoader(
			m_jar,
			m_mappings.getTranslator( TranslationDirection.Deobfuscating ),
			m_mappings.getTranslator( TranslationDirection.Obfuscating )
		) );
		m_settings.setForceExplicitImports( true );
		m_settings.setShowSyntheticMembers( true );
	}
	
	public String getJarName( )
	{
		return m_file.getName();
	}
	
	public List<ClassFile> getObfuscatedClasses( )
	{
		List<ClassFile> classes = new ArrayList<ClassFile>();
		Enumeration<JarEntry> entries = m_jar.entries();
		while( entries.hasMoreElements() )
		{
			JarEntry entry = entries.nextElement();
			
			// get the class name
			String className = toClassName( entry.getName() );
			if( className == null )
			{
				continue;
			}
			
			ClassFile classFile = new ClassFile( className );
			if( classFile.isObfuscated() )
			{
				classes.add( classFile );
			}
		}
		Collections.sort( classes, m_obfuscatedClassSorter );
		return classes;
	}
	
	// TODO: could go somewhere more generic
	private static String toClassName( String fileName )
	{
		final String suffix = ".class";
		
		if( !fileName.endsWith( suffix ) )
		{
			return null;
		}
		
		return fileName.substring( 0, fileName.length() - suffix.length() ).replace( "/", "." );
	}
	
	public String getSource( final ClassFile classFile )
	{
		StringWriter buf = new StringWriter();
		Decompiler.decompile( classFile.getName(), new PlainTextOutput( buf ), m_settings );
		return buf.toString();
	}
	
	// NOTE: these methods are a bit messy... oh well

	public void rename( Entry entry, String newName )
	{
		if( entry instanceof ClassEntry )
		{
			m_mappings.setClassName( (ClassEntry)entry, newName );
		}
		else if( entry instanceof FieldEntry )
		{
			m_mappings.setFieldName( (FieldEntry)entry, newName );
		}
		else if( entry instanceof MethodEntry )
		{
			m_mappings.setMethodName( (MethodEntry)entry, newName );
		}
		else if( entry instanceof ArgumentEntry )
		{
			m_mappings.setArgumentName( (ArgumentEntry)entry, newName );
		}
		else
		{
			throw new Error( "Unknown entry type: " + entry.getClass().getName() );
		}
	}
	
	public Entry obfuscate( Entry in )
	{
		Translator translator = m_mappings.getTranslator( TranslationDirection.Obfuscating );
		if( in instanceof ClassEntry )
		{
			return translator.translateEntry( (ClassEntry)in );
		}
		else if( in instanceof FieldEntry )
		{
			return translator.translateEntry( (FieldEntry)in );
		}
		else if( in instanceof MethodEntry )
		{
			return translator.translateEntry( (MethodEntry)in );
		}
		else if( in instanceof ArgumentEntry )
		{
			return translator.translateEntry( (ArgumentEntry)in );
		}
		else
		{
			throw new Error( "Unknown entry type: " + in.getClass().getName() );
		}
	}
	
	public Entry deobfuscate( Entry in )
	{
		Translator translator = m_mappings.getTranslator( TranslationDirection.Deobfuscating );
		if( in instanceof ClassEntry )
		{
			return translator.translateEntry( (ClassEntry)in );
		}
		else if( in instanceof FieldEntry )
		{
			return translator.translateEntry( (FieldEntry)in );
		}
		else if( in instanceof MethodEntry )
		{
			return translator.translateEntry( (MethodEntry)in );
		}
		else if( in instanceof ArgumentEntry )
		{
			return translator.translateEntry( (ArgumentEntry)in );
		}
		else
		{
			throw new Error( "Unknown entry type: " + in.getClass().getName() );
		}
	}
}
