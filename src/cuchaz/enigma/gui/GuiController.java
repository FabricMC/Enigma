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
package cuchaz.enigma.gui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cuchaz.enigma.ClassFile;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.Analyzer;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryPair;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;

public class GuiController
{
	private Deobfuscator m_deobfuscator;
	private Gui m_gui;
	private SourceIndex m_index;
	private ClassFile m_currentFile;
	
	public GuiController( Gui gui )
	{
		m_gui = gui;
		m_deobfuscator = null;
		m_index = null;
		m_currentFile = null;
	}
	
	public void openJar( File file )
	throws IOException
	{
		m_deobfuscator = new Deobfuscator( file );
		m_gui.onOpenJar( m_deobfuscator.getJarName() );
		refreshClasses();
	}
	
	public void closeJar( )
	{
		m_deobfuscator = null;
		m_gui.onCloseJar();
	}
	
	public void openMappings( File file )
	throws IOException
	{
		FileReader in = new FileReader( file );
		m_deobfuscator.setMappings( new MappingsReader().read( in ) );
		in.close();
		m_gui.setMappingsLoaded( true );
		refreshClasses();
		refreshOpenFiles();
	}

	public void saveMappings( File file )
	throws IOException
	{
		FileWriter out = new FileWriter( file );
		new MappingsWriter().write( out, m_deobfuscator.getMappings() );
		out.close();
	}

	public void closeMappings( )
	{
		m_deobfuscator.setMappings( null );
		m_gui.setMappingsLoaded( false );
		refreshOpenFiles();
	}
	
	public void deobfuscateClass( ClassFile classFile )
	{
		m_currentFile = classFile;
		deobfuscate( m_currentFile );
	}
	
	public EntryPair getEntryPair( int pos )
	{
		if( m_index == null )
		{
			return null;
		}
		
		Entry deobfEntry = m_index.getEntry( pos );
		if( deobfEntry == null )
		{
			return null;
		}
		return new EntryPair( m_deobfuscator.obfuscate( deobfEntry ), deobfEntry );
	}
	
	public void rename( Entry obfsEntry, String newName )
	{
		m_deobfuscator.rename( obfsEntry, newName );
		
		// did we rename the current file?
		if( obfsEntry instanceof ClassEntry )
		{
			ClassEntry classEntry = (ClassEntry)obfsEntry;
			
			// update the current file
			if( classEntry.getName().equals( m_currentFile.getName() ) )
			{
				m_currentFile = new ClassFile( newName );
			}
		}
		
		refreshOpenFiles();
	}
	
	private void refreshClasses( )
	{
		List<ClassFile> obfClasses = new ArrayList<ClassFile>();
		List<ClassFile> deobfClasses = new ArrayList<ClassFile>();
		m_deobfuscator.getSortedClasses( obfClasses, deobfClasses );
		m_gui.setObfClasses( obfClasses );
		m_gui.setDeobfClasses( deobfClasses );
	}
	
	private void refreshOpenFiles( )
	{
		if( m_currentFile != null )
		{
			deobfuscate( m_currentFile );
		}
	}

	private void deobfuscate( final ClassFile classFile )
	{
		m_gui.setSource( "(deobfuscating...)" );
		
		// run the deobfuscator in a separate thread so we don't block the GUI event queue
		new Thread( )
		{
			@Override
			public void run( )
			{
				// deobfuscate the bytecode
				String source = m_deobfuscator.getSource( classFile );
				m_gui.setSource( source );
				
				// index the source file
				m_index = Analyzer.analyze( classFile.getName(), source );
				m_gui.setHighlightedTokens( m_index.tokens() );
			}
		}.start();
	}
}
