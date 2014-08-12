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
import java.util.List;
import java.util.Stack;

import com.google.common.collect.Lists;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryPair;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.TranslationDirection;

public class GuiController
{
	private Deobfuscator m_deobfuscator;
	private Gui m_gui;
	private SourceIndex m_index;
	private ClassEntry m_currentClass;
	private boolean m_isDirty;
	private Stack<Entry> m_entryStack;
	
	public GuiController( Gui gui )
	{
		m_gui = gui;
		m_deobfuscator = null;
		m_index = null;
		m_currentClass = null;
		m_isDirty = false;
		m_entryStack = new Stack<Entry>();
	}
	
	public boolean isDirty( )
	{
		return m_isDirty;
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
		m_isDirty = false;
		m_gui.setMappingsFile( file );
		refreshClasses();
		refreshCurrentClass();
	}

	public void saveMappings( File file )
	throws IOException
	{
		FileWriter out = new FileWriter( file );
		new MappingsWriter().write( out, m_deobfuscator.getMappings() );
		out.close();
		m_isDirty = false;
	}

	public void closeMappings( )
	{
		m_deobfuscator.setMappings( null );
		m_gui.setMappingsFile( null );
		refreshClasses();
		refreshCurrentClass();
	}
	
	public Token getToken( int pos )
	{
		if( m_index == null )
		{
			return null;
		}
		
		return m_index.getToken( pos );
	}
	
	public EntryPair<Entry> getEntryPair( Token token )
	{
		if( m_index == null )
		{
			return null;
		}
		
		Entry deobfEntry = m_index.getEntry( token );
		return new EntryPair<Entry>( m_deobfuscator.obfuscateEntry( deobfEntry ), deobfEntry );
	}
	
	public boolean entryHasMapping( Entry deobfEntry )
	{
		return m_deobfuscator.hasMapping( m_deobfuscator.obfuscateEntry( deobfEntry ) );
	}
	
	public boolean entryIsObfuscatedIdenfitier( Entry deobfEntry )
	{
		return m_deobfuscator.entryIsObfuscatedIdenfitier( m_deobfuscator.obfuscateEntry( deobfEntry ) );
	}
	
	public ClassInheritanceTreeNode getClassInheritance( ClassEntry obfClassEntry )
	{
		ClassInheritanceTreeNode rootNode = m_deobfuscator.getJarIndex().getClassInheritance(
			m_deobfuscator.getTranslator( TranslationDirection.Deobfuscating ),
			obfClassEntry
		);
		return ClassInheritanceTreeNode.findNode( rootNode, obfClassEntry );
	}
	
	public MethodInheritanceTreeNode getMethodInheritance( MethodEntry obfMethodEntry )
	{
		MethodInheritanceTreeNode rootNode = m_deobfuscator.getJarIndex().getMethodInheritance(
			m_deobfuscator.getTranslator( TranslationDirection.Deobfuscating ),
			obfMethodEntry
		);
		return MethodInheritanceTreeNode.findNode( rootNode, obfMethodEntry );
	}
	
	public void rename( Entry obfEntry, String newName )
	{
		m_deobfuscator.rename( obfEntry, newName );
		m_isDirty = true;
		refreshClasses();
		refreshCurrentClass( obfEntry );
	}
	
	public void openEntry( Entry entry )
	{
		// go to the entry
		Entry obfEntry = m_deobfuscator.obfuscateEntry( entry );
		if( m_currentClass == null || !m_currentClass.equals( obfEntry.getClassEntry() ) )
		{
			m_currentClass = new ClassEntry( obfEntry.getClassEntry() );
			deobfuscate( m_currentClass, obfEntry );
		}
		else
		{
			m_gui.showToken( m_index.getDeclarationToken( m_deobfuscator.deobfuscateEntry( obfEntry ) ) );
		}
		
		if( m_entryStack.isEmpty() || !m_entryStack.peek().equals( obfEntry ) )
		{
			// update the stack
			m_entryStack.push( obfEntry );
		}
	}
	
	public void openPreviousEntry( )
	{
		if( hasPreviousEntry() )
		{
			m_entryStack.pop();
			openEntry( m_entryStack.peek() );
		}
	}
	
	public boolean hasPreviousEntry( )
	{
		return m_entryStack.size() > 1;
	}
	
	private void refreshClasses( )
	{
		List<String> obfClasses = Lists.newArrayList();
		List<String> deobfClasses = Lists.newArrayList();
		m_deobfuscator.getSeparatedClasses( obfClasses, deobfClasses );
		m_gui.setObfClasses( obfClasses );
		m_gui.setDeobfClasses( deobfClasses );
	}
	
	private void refreshCurrentClass( )
	{
		refreshCurrentClass( null );
	}
	
	private void refreshCurrentClass( Entry obfEntryToShow )
	{
		if( m_currentClass != null )
		{
			deobfuscate( m_currentClass, obfEntryToShow );
		}
	}
	
	private void deobfuscate( final ClassEntry classEntry, final Entry obfEntryToShow )
	{
		m_gui.setSource( "(deobfuscating...)" );
		
		// run the deobfuscator in a separate thread so we don't block the GUI event queue
		new Thread( )
		{
			@Override
			public void run( )
			{
				// decompile,deobfuscate the bytecode
				m_index = m_deobfuscator.getSource( classEntry.getClassName() );
				m_gui.setSource( m_index.getSource() );
				if( obfEntryToShow != null )
				{
					m_gui.showToken( m_index.getDeclarationToken( m_deobfuscator.deobfuscateEntry( obfEntryToShow ) ) );
				}
				
				// set the highlighted tokens
				List<Token> obfuscatedTokens = Lists.newArrayList();
				List<Token> deobfuscatedTokens = Lists.newArrayList();
				for( Token token : m_index.tokens() )
				{
					Entry entry = m_index.getEntry( token );
					if( entryHasMapping( entry ) )
					{
						deobfuscatedTokens.add( token );
					}
					else if( entryIsObfuscatedIdenfitier( entry ) )
					{
						obfuscatedTokens.add( token );
					}
				}
				m_gui.setHighlightedTokens( obfuscatedTokens, deobfuscatedTokens );
			}
		}.start();
	}
}
