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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import cuchaz.enigma.mapping.Ancestries;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Renamer;
import cuchaz.enigma.mapping.TranslationDirection;
import cuchaz.enigma.mapping.Translator;

public class Deobfuscator
{
	private File m_file;
	private JarFile m_jar;
	private DecompilerSettings m_settings;
	private Ancestries m_ancestries;
	private Mappings m_mappings;
	private Renamer m_renamer;
	
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
		
		// config the decompiler
		m_settings = DecompilerSettings.javaDefaults();
		m_settings.setForceExplicitImports( true );
		m_settings.setShowSyntheticMembers( true );
		
		// init mappings
		setMappings( new Mappings() );
	}
	
	public String getJarName( )
	{
		return m_file.getName();
	}
	
	public Mappings getMappings( )
	{
		return m_mappings;
	}
	public void setMappings( Mappings val )
	{
		if( val == null )
		{
			val = new Mappings();
		}
		m_mappings = val;
		m_renamer = new Renamer( m_ancestries, m_mappings );
		
		// update decompiler options
		m_settings.setTypeLoader( new TranslatingTypeLoader(
			m_jar,
			m_mappings.getTranslator( m_ancestries, TranslationDirection.Obfuscating ),
			m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating )
		) );
	}
	
	public void getSeparatedClasses( List<ClassFile> obfClasses, Map<ClassFile,String> deobfClasses )
	{
		Enumeration<JarEntry> entries = m_jar.entries();
		while( entries.hasMoreElements() )
		{
			JarEntry entry = entries.nextElement();
			
			// skip everything but class files
			if( !entry.getName().endsWith( ".class" ) )
			{
				continue;
			}
			
			// get the class name from the file
			String className = entry.getName().substring( 0, entry.getName().length() - 6 );
			ClassFile classFile = new ClassFile( className );
			
			// separate the classes
			ClassMapping classMapping = m_mappings.getClassByObf( classFile.getName() );
			if( classMapping != null )
			{
				deobfClasses.put( classFile, classMapping.getDeobfName() );
			}
			else
			{
				obfClasses.add( classFile );
			}
		}
	}
	
	public String getSource( final ClassFile classFile )
	{
		// is this class deobfuscated?
		// we need to tell the decompiler the deobfuscated name so it doesn't get freaked out
		// the decompiler only sees the deobfuscated class, so we need to load it by the deobfuscated name
		String deobfName = classFile.getName();
		ClassMapping classMapping = m_mappings.getClassByObf( classFile.getName() );
		if( classMapping != null )
		{
			deobfName = classMapping.getDeobfName();
		}
		
		// decompile it!
		StringWriter buf = new StringWriter();
		Decompiler.decompile( deobfName, new PlainTextOutput( buf ), m_settings );
		return buf.toString();
	}
	
	// NOTE: these methods are a bit messy... oh well

	public void rename( Entry obfEntry, String newName )
	{
		if( obfEntry instanceof ClassEntry )
		{
			m_renamer.setClassName( (ClassEntry)obfEntry, newName );
		}
		else if( obfEntry instanceof FieldEntry )
		{
			m_renamer.setFieldName( (FieldEntry)obfEntry, newName );
		}
		else if( obfEntry instanceof MethodEntry )
		{
			m_renamer.setMethodName( (MethodEntry)obfEntry, newName );
		}
		else if( obfEntry instanceof ArgumentEntry )
		{
			m_renamer.setArgumentName( (ArgumentEntry)obfEntry, newName );
		}
		else
		{
			throw new Error( "Unknown entry type: " + obfEntry.getClass().getName() );
		}
	}
	
	public Entry obfuscateEntry( Entry deobfEntry )
	{
		Translator translator = m_mappings.getTranslator( m_ancestries, TranslationDirection.Obfuscating );
		if( deobfEntry instanceof ClassEntry )
		{
			return translator.translateEntry( (ClassEntry)deobfEntry );
		}
		else if( deobfEntry instanceof FieldEntry )
		{
			return translator.translateEntry( (FieldEntry)deobfEntry );
		}
		else if( deobfEntry instanceof MethodEntry )
		{
			return translator.translateEntry( (MethodEntry)deobfEntry );
		}
		else if( deobfEntry instanceof ArgumentEntry )
		{
			return translator.translateEntry( (ArgumentEntry)deobfEntry );
		}
		else
		{
			throw new Error( "Unknown entry type: " + deobfEntry.getClass().getName() );
		}
	}
	
	public Entry deobfuscateEntry( Entry obfEntry )
	{
		Translator translator = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating );
		if( obfEntry instanceof ClassEntry )
		{
			return translator.translateEntry( (ClassEntry)obfEntry );
		}
		else if( obfEntry instanceof FieldEntry )
		{
			return translator.translateEntry( (FieldEntry)obfEntry );
		}
		else if( obfEntry instanceof MethodEntry )
		{
			return translator.translateEntry( (MethodEntry)obfEntry );
		}
		else if( obfEntry instanceof ArgumentEntry )
		{
			return translator.translateEntry( (ArgumentEntry)obfEntry );
		}
		else
		{
			throw new Error( "Unknown entry type: " + obfEntry.getClass().getName() );
		}
	}
	
	public boolean hasMapping( Entry obfEntry )
	{
		Translator translator = m_mappings.getTranslator( m_ancestries, TranslationDirection.Deobfuscating );
		if( obfEntry instanceof ClassEntry )
		{
			String deobfName = translator.translate( (ClassEntry)obfEntry );
			return deobfName != null && !deobfName.equals( obfEntry.getName() );
		}
		else if( obfEntry instanceof FieldEntry )
		{
			String deobfName = translator.translate( (FieldEntry)obfEntry );
			return deobfName != null && !deobfName.equals( obfEntry.getName() );
		}
		else if( obfEntry instanceof MethodEntry )
		{
			String deobfName = translator.translate( (MethodEntry)obfEntry );
			return deobfName != null && !deobfName.equals( obfEntry.getName() );
		}
		else if( obfEntry instanceof ArgumentEntry )
		{
			return translator.translate( (ArgumentEntry)obfEntry ) != null;
		}
		else
		{
			throw new Error( "Unknown entry type: " + obfEntry.getClass().getName() );
		}
	}
}
