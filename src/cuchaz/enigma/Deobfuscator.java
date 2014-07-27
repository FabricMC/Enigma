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
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.strobel.assembler.metadata.JarTypeLoader;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

public class Deobfuscator
{
	private File m_file;
	private JarFile m_jar;
	private DecompilerSettings m_settings;
	
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
		m_settings = DecompilerSettings.javaDefaults();
		m_settings.setTypeLoader( new JarTypeLoader( m_jar ) );
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
}
