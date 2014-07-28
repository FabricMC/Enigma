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

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import cuchaz.enigma.analysis.Analyzer;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.gui.ClassSelectionListener;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.RenameListener;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryPair;

public class Controller implements ClassSelectionListener, CaretListener, RenameListener
{
	private Deobfuscator m_deobfuscator;
	private Gui m_gui;
	private SourceIndex m_index;
	private ClassFile m_currentFile;
	
	public Controller( Deobfuscator deobfuscator, Gui gui )
	{
		m_deobfuscator = deobfuscator;
		m_gui = gui;
		m_index = null;
		m_currentFile = null;
		
		// update GUI
		gui.setTitle( deobfuscator.getJarName() );
		gui.setObfClasses( deobfuscator.getObfuscatedClasses() );
		
		// handle events
		gui.setClassSelectionListener( this );
		gui.setCaretListener( this );
		gui.setRenameListener( this );
	}
	
	@Override
	public void classSelected( ClassFile classFile )
	{
		m_currentFile = classFile;
		deobfuscate( m_currentFile );
	}
	
	@Override
	public void caretUpdate( CaretEvent event )
	{
		if( m_index != null )
		{
			int pos = event.getDot();
			Entry deobfEntry = m_index.getEntry( pos );
			if( deobfEntry != null )
			{
				m_gui.showEntryPair( new EntryPair( m_deobfuscator.obfuscate( deobfEntry ), deobfEntry ) );
			}
			else
			{
				m_gui.clearEntryPair();
			}
		}
	}
	
	@Override
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
		
		deobfuscate( m_currentFile );
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
				m_gui.highlightTokens( m_index.tokens() );
			}
		}.start();
	}
}
