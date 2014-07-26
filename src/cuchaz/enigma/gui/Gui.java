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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.Token;
import cuchaz.enigma.ClassFile;
import cuchaz.enigma.analysis.SourceIndex;

public class Gui
{
	private static final String Name = "Enigma";
	
	// controls
	private JFrame m_frame;
	private JList<ClassFile> m_obfClasses;
	private JList<ClassFile> m_deobfClasses;
	private JEditorPane m_editor;
	
	// handlers
	private ClassSelectionHandler m_classSelectionHandler;
	
	private BoxHighlightPainter m_highlightPainter;
	
	public Gui( )
	{
		// init frame
		m_frame = new JFrame( Name );
		final Container pane = m_frame.getContentPane();
		pane.setLayout( new BorderLayout() );
		
		// init obfuscated classes list
		m_obfClasses = new JList<ClassFile>();
		m_obfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_obfClasses.setLayoutOrientation( JList.VERTICAL );
		m_obfClasses.setCellRenderer( new ObfuscatedClassListCellRenderer() );
		m_obfClasses.addMouseListener( new MouseAdapter()
		{
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					if( m_classSelectionHandler != null )
					{
						ClassFile selected = m_obfClasses.getSelectedValue();
						if( selected != null )
						{
							m_classSelectionHandler.classSelected( selected );
						}
					}
				}
			}
		} );
		JScrollPane obfScroller = new JScrollPane( m_obfClasses );
		JPanel obfPanel = new JPanel();
		obfPanel.setLayout( new BorderLayout() );
		obfPanel.add( new JLabel( "Obfuscated Classes" ), BorderLayout.NORTH );
		obfPanel.add( obfScroller, BorderLayout.CENTER );
		
		// init deobfuscated classes list
		m_deobfClasses = new JList<ClassFile>();
		m_obfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_obfClasses.setLayoutOrientation( JList.VERTICAL );
		JScrollPane deobfScroller = new JScrollPane( m_deobfClasses );
		JPanel deobfPanel = new JPanel();
		deobfPanel.setLayout( new BorderLayout() );
		deobfPanel.add( new JLabel( "De-obfuscated Classes" ), BorderLayout.NORTH );
		deobfPanel.add( deobfScroller, BorderLayout.CENTER );
		
		// init editor
		DefaultSyntaxKit.initKit();
		m_editor = new JEditorPane();
		m_editor.setEditable( false );
		JScrollPane sourceScroller = new JScrollPane( m_editor );
		m_editor.setContentType( "text/java" );
		
		// layout controls
		JSplitPane splitLeft = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true, obfPanel, deobfPanel );
		JSplitPane splitMain = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, sourceScroller );
		pane.add( splitMain, BorderLayout.CENTER );
		
		// show the frame
		pane.doLayout();
		m_frame.setSize( 800, 600 );
		m_frame.setVisible( true );
		m_frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
		
		// init handlers
		m_classSelectionHandler = null;
		
		m_highlightPainter = new BoxHighlightPainter();
	}
	
	public void setTitle( String title )
	{
		m_frame.setTitle( Name + " - " + title );
	}
	
	public void setObfClasses( List<ClassFile> classes )
	{
		m_obfClasses.setListData( new Vector<ClassFile>( classes ) );
	}
	
	public void setSource( String source )
	{
		setSource( source, null );
	}
	
	public void setSource( String source, SourceIndex index )
	{
		m_editor.setText( source );
		
		// remove any old highlighters
		m_editor.getHighlighter().removeAllHighlights();;
		
		if( index != null )
		{
			// color things based on the index
			for( Token token : index.tokens() )
			{
				try
				{
					m_editor.getHighlighter().addHighlight( token.start, token.end(), m_highlightPainter );
				}
				catch( BadLocationException ex )
				{
					throw new Error( ex );
				}
			}
		}
	}
	
	public void setClassSelectionHandler( ClassSelectionHandler val )
	{
		m_classSelectionHandler = val;
	}
}
