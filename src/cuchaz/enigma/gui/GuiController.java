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
import java.util.Deque;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.BehaviorReferenceTreeNode;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.FieldReferenceTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.TranslationDirection;

public class GuiController
{
	private Deobfuscator m_deobfuscator;
	private Gui m_gui;
	private SourceIndex m_index;
	private ClassEntry m_currentObfClass;
	private boolean m_isDirty;
	private Deque<EntryReference<Entry,Entry>> m_referenceStack; // TODO: make a location class, can be either Entry or EntryReference
	
	public GuiController( Gui gui )
	{
		m_gui = gui;
		m_deobfuscator = null;
		m_index = null;
		m_currentObfClass = null;
		m_isDirty = false;
		m_referenceStack = Queues.newArrayDeque();
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
	throws IOException, MappingParseException
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
		
		return m_index.getReferenceToken( pos );
	}
	
	public EntryReference<Entry,Entry> getDeobfReference( Token token )
	{
		if( m_index == null )
		{
			return null;
		}
		return m_index.getDeobfReference( token );
	}
	
	public boolean entryHasMapping( Entry deobfEntry )
	{
		return m_deobfuscator.hasMapping( m_deobfuscator.obfuscateEntry( deobfEntry ) );
	}
	
	public boolean entryIsObfuscatedIdenfitier( Entry deobfEntry )
	{
		return m_deobfuscator.isObfuscatedIdentifier( m_deobfuscator.obfuscateEntry( deobfEntry ) );
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
	
	public FieldReferenceTreeNode getFieldReferences( FieldEntry obfFieldEntry )
	{
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(
			m_deobfuscator.getTranslator( TranslationDirection.Deobfuscating ),
			obfFieldEntry
		);
		rootNode.load( m_deobfuscator.getJarIndex(), true );
		return rootNode;
	}
	
	public BehaviorReferenceTreeNode getMethodReferences( BehaviorEntry obfEntry )
	{
		BehaviorReferenceTreeNode rootNode = new BehaviorReferenceTreeNode(
			m_deobfuscator.getTranslator( TranslationDirection.Deobfuscating ),
			obfEntry
		);
		rootNode.load( m_deobfuscator.getJarIndex(), true );
		return rootNode;
	}
	
	public void rename( EntryReference<Entry,Entry> deobfReference, String newName )
	{
		EntryReference<Entry,Entry> obfReference = m_deobfuscator.obfuscateReference( deobfReference );
		m_deobfuscator.rename( obfReference.entry, newName );
		m_isDirty = true;
		refreshClasses();
		refreshCurrentClass( obfReference );
	}
	
	public void openDeclaration( Entry entry )
	{
		if( entry == null )
		{
			throw new IllegalArgumentException( "Entry cannot be null!" );
		}
		openReference( new EntryReference<Entry,Entry>( entry ) );
	}
	
	public void openReference( EntryReference<Entry,Entry> deobfReference )
	{
		if( deobfReference == null )
		{
			throw new IllegalArgumentException( "Reference cannot be null!" );
		}
		
		// get the reference target class
		EntryReference<Entry,Entry> obfReference = m_deobfuscator.obfuscateReference( deobfReference );
		ClassEntry obfClassEntry = obfReference.getClassEntry().getOuterClassEntry();
		if( m_currentObfClass == null || !m_currentObfClass.equals( obfClassEntry ) )
		{
			// deobfuscate the class, then navigate to the reference
			m_currentObfClass = obfClassEntry;
			deobfuscate( m_currentObfClass, obfReference );
		}
		else
		{
			// the class file is already open, just navigate to the reference
			m_gui.showToken( m_index.getReferenceToken( deobfReference ) );
		}
	}
	
	public void savePreviousReference( EntryReference<Entry,Entry> deobfReference )
	{
		m_referenceStack.push( m_deobfuscator.obfuscateReference( deobfReference ) );
	}
	
	public void openPreviousReference( )
	{
		if( hasPreviousLocation() )
		{
			openReference( m_deobfuscator.deobfuscateReference( m_referenceStack.pop() ) );
		}
	}
	
	public boolean hasPreviousLocation( )
	{
		return !m_referenceStack.isEmpty();
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
	
	private void refreshCurrentClass( EntryReference<Entry,Entry> obfReferenceToShow )
	{
		if( m_currentObfClass != null )
		{
			deobfuscate( m_currentObfClass, obfReferenceToShow );
		}
	}
	
	private void deobfuscate( final ClassEntry classEntry, final EntryReference<Entry,Entry> obfReferenceToShow )
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
				if( obfReferenceToShow != null )
				{
					EntryReference<Entry,Entry> deobfReferenceToShow = m_deobfuscator.deobfuscateReference( obfReferenceToShow );
					Token token = m_index.getReferenceToken( deobfReferenceToShow );
					if( token == null )
					{
						// DEBUG
						System.out.println( "WARNING: can't find token for " + obfReferenceToShow + " -> " + deobfReferenceToShow );
					}
					else
					{
						m_gui.showToken( token );
					}
				}
				
				// set the highlighted tokens
				List<Token> obfuscatedTokens = Lists.newArrayList();
				List<Token> deobfuscatedTokens = Lists.newArrayList();
				for( Token token : m_index.referenceTokens() )
				{
					EntryReference<Entry,Entry> reference = m_index.getDeobfReference( token );
					if( entryHasMapping( reference.entry ) )
					{
						deobfuscatedTokens.add( token );
					}
					else if( entryIsObfuscatedIdenfitier( reference.entry ) )
					{
						obfuscatedTokens.add( token );
					}
				}
				m_gui.setHighlightedTokens( obfuscatedTokens, deobfuscatedTokens );
			}
		}.start();
	}
}
