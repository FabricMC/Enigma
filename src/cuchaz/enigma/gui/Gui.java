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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.Token;
import cuchaz.enigma.ClassFile;
import cuchaz.enigma.Constants;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryPair;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class Gui
{
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
	
	private GuiController m_controller;
	
	// controls
	private JFrame m_frame;
	private JList<ClassFile> m_obfClasses;
	private JList<ClassFile> m_deobfClasses;
	private JEditorPane m_editor;
	private JPanel m_actionPanel;
	private JPanel m_renamePanel;
	private JLabel m_typeLabel;
	private JTextField m_nameField;
	private JButton m_renameButton;
	private BoxHighlightPainter m_highlightPainter;
	
	// dynamic menu items
	private JMenuItem m_closeJarMenu;
	private JMenuItem m_openMappingsMenu;
	private JMenuItem m_saveMappingsMenu;
	private JMenuItem m_saveMappingsAsMenu;
	private JMenuItem m_closeMappingsMenu;
	
	// state
	private EntryPair<Entry> m_selectedEntryPair;
	private JFileChooser m_jarFileChooser;
	private JFileChooser m_mappingsFileChooser;
	
	public Gui( )
	{
		m_controller = new GuiController( this );
		
		// init file choosers
		m_jarFileChooser = new JFileChooser();
		m_mappingsFileChooser = new JFileChooser();
		
		// init frame
		m_frame = new JFrame( Constants.Name );
		final Container pane = m_frame.getContentPane();
		pane.setLayout( new BorderLayout() );
		
		// init obfuscated classes list
		m_obfClasses = new JList<ClassFile>();
		m_obfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_obfClasses.setLayoutOrientation( JList.VERTICAL );
		m_obfClasses.setCellRenderer( new ClassListCellRenderer() );
		m_obfClasses.addMouseListener( new MouseAdapter()
		{
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					ClassFile selected = m_obfClasses.getSelectedValue();
					if( selected != null )
					{
						m_controller.deobfuscateClass( selected );
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
		m_deobfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_deobfClasses.setLayoutOrientation( JList.VERTICAL );
		m_deobfClasses.setCellRenderer( new ClassListCellRenderer() );
		m_deobfClasses.addMouseListener( new MouseAdapter()
		{
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					ClassFile selected = m_deobfClasses.getSelectedValue();
					if( selected != null )
					{
						m_controller.deobfuscateClass( selected );
					}
				}
			}
		} );
		JScrollPane deobfScroller = new JScrollPane( m_deobfClasses );
		JPanel deobfPanel = new JPanel();
		deobfPanel.setLayout( new BorderLayout() );
		deobfPanel.add( new JLabel( "De-obfuscated Classes" ), BorderLayout.NORTH );
		deobfPanel.add( deobfScroller, BorderLayout.CENTER );
		
		// init action panel
		m_actionPanel = new JPanel();
		m_actionPanel.setLayout( new BoxLayout( m_actionPanel, BoxLayout.Y_AXIS ) );
		m_actionPanel.setPreferredSize( new Dimension( 0, 120 ) );
		m_actionPanel.setBorder( BorderFactory.createTitledBorder( "Identifier Info" ) );
		m_nameField = new JTextField( 26 );
		m_renameButton = new JButton( "Rename" );
		m_renameButton.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				if( m_selectedEntryPair != null )
				{
					m_controller.rename( m_selectedEntryPair.obf, m_nameField.getText() );
				}
			}
		} );
		m_renamePanel = new JPanel();
		m_renamePanel.setLayout( new FlowLayout( FlowLayout.LEFT, 6, 0 ) );
		m_typeLabel = new JLabel( "LongName:", JLabel.RIGHT );
		// NOTE: this looks ridiculous, but it fixes the label size to the size of current text
		m_typeLabel.setPreferredSize( m_typeLabel.getPreferredSize() );
		m_renamePanel.add( m_typeLabel );
		m_renamePanel.add( m_nameField );
		m_renamePanel.add( m_renameButton );
		clearEntryPair();
		
		// init editor
		DefaultSyntaxKit.initKit();
		m_highlightPainter = new BoxHighlightPainter();
		m_editor = new JEditorPane();
		m_editor.setEditable( false );
		JScrollPane sourceScroller = new JScrollPane( m_editor );
		m_editor.setContentType( "text/java" );
		m_editor.addCaretListener( new CaretListener( )
		{
			@Override
			public void caretUpdate( CaretEvent event )
			{
				m_selectedEntryPair = m_controller.getEntryPair( event.getDot() );
				if( m_selectedEntryPair != null )
				{
					showEntryPair( m_selectedEntryPair );
				}
				else
				{
					clearEntryPair();
				}
			}
		} );
		
		// layout controls
		JSplitPane splitLeft = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true, obfPanel, deobfPanel );
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout( new BorderLayout() );
		rightPanel.add( m_actionPanel, BorderLayout.NORTH );
		rightPanel.add( sourceScroller, BorderLayout.CENTER );
		JSplitPane splitMain = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, rightPanel );
		pane.add( splitMain, BorderLayout.CENTER );
		
		// init menus
		JMenuBar menuBar = new JMenuBar();
		m_frame.setJMenuBar( menuBar );
		{
			JMenu menu = new JMenu( "File" );
			menuBar.add( menu );
			{
				JMenuItem item = new JMenuItem( "Open Jar..." );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						if( m_jarFileChooser.showOpenDialog( m_frame ) == JFileChooser.APPROVE_OPTION )
						{
							try
							{
								m_controller.openJar( m_jarFileChooser.getSelectedFile() );
							}
							catch( IOException ex )
							{
								throw new Error( ex );
							}
						}
					}
				} );
			}
			{
				JMenuItem item = new JMenuItem( "Close Jar" );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						m_controller.closeJar();
					}
				} );
				m_closeJarMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem( "Open Mappings..." );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						if( m_mappingsFileChooser.showOpenDialog( m_frame ) == JFileChooser.APPROVE_OPTION )
						{
							try
							{
								m_controller.openMappings( m_mappingsFileChooser.getSelectedFile() );
							}
							catch( IOException ex )
							{
								throw new Error( ex );
							}
						}
					}
				} );
				m_openMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem( "Save Mappings" );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						try
						{
							m_controller.saveMappings( m_mappingsFileChooser.getSelectedFile() );
						}
						catch( IOException ex )
						{
							throw new Error( ex );
						}
					}
				} );
				m_saveMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem( "Save Mappings As..." );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						if( m_mappingsFileChooser.showSaveDialog( m_frame ) == JFileChooser.APPROVE_OPTION )
						{
							try
							{
								m_controller.saveMappings( m_mappingsFileChooser.getSelectedFile() );
								m_saveMappingsMenu.setEnabled( true );
							}
							catch( IOException ex )
							{
								throw new Error( ex );
							}
						}
					}
				} );
				m_saveMappingsAsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem( "Close Mapppings" );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						m_controller.closeMappings();
					}
				} );
				m_closeMappingsMenu = item;
			}
		}
		{
			JMenu menu = new JMenu( "Help" );
			menuBar.add( menu );
			{
				JMenuItem item = new JMenuItem( "About" );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						AboutDialog.show( m_frame );
					}
				} );
			}
		}
		
		// init state
		onCloseJar();
		
		// show the frame
		pane.doLayout();
		m_frame.setSize( 800, 600 );
		m_frame.setMinimumSize( new Dimension( 640, 480 ) );
		m_frame.setVisible( true );
		m_frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
	}
	
	public GuiController getController( )
	{
		return m_controller;
	}
	
	public void onOpenJar( String jarName )
	{
		// update gui
		m_frame.setTitle( Constants.Name + " - " + jarName );
		setSource( null );
		
		// update menu
		m_closeJarMenu.setEnabled( true );
		m_openMappingsMenu.setEnabled( true );
		m_saveMappingsMenu.setEnabled( false );
		m_saveMappingsAsMenu.setEnabled( true );
		m_closeMappingsMenu.setEnabled( true );
	}
	
	public void onCloseJar( )
	{
		// update gui
		m_frame.setTitle( Constants.Name );
		setObfClasses( null );
		setSource( null );
		
		// update menu
		m_closeJarMenu.setEnabled( false );
		m_openMappingsMenu.setEnabled( false );
		m_saveMappingsMenu.setEnabled( false );
		m_saveMappingsAsMenu.setEnabled( false );
		m_closeMappingsMenu.setEnabled( false );
	}
	
	public void setObfClasses( List<ClassFile> classes )
	{
		if( classes != null )
		{
			Vector<ClassFile> sortedClasses = new Vector<ClassFile>( classes );
			Collections.sort( sortedClasses, m_obfuscatedClassSorter );
			m_obfClasses.setListData( sortedClasses );
		}
		else
		{
			m_obfClasses.setListData( new Vector<ClassFile>() );
		}
	}
	
	public void setDeobfClasses( List<ClassFile> classes )
	{
		if( classes != null )
		{
			m_deobfClasses.setListData( new Vector<ClassFile>( classes ) );
		}
		else
		{
			m_deobfClasses.setListData( new Vector<ClassFile>() );
		}
	}
	
	public void setMappingsFile( File file )
	{
		m_mappingsFileChooser.setSelectedFile( file );
		m_saveMappingsMenu.setEnabled( file != null );
	}
	
	public void setSource( String source )
	{
		setSource( source, null );
	}
	
	public void setSource( String source, SourceIndex index )
	{
		m_editor.setText( source );
		setHighlightedTokens( null );
	}
	
	public void setHighlightedTokens( Iterable<Token> tokens )
	{
		// remove any old highlighters
		m_editor.getHighlighter().removeAllHighlights();
		
		if( tokens == null )
		{
			return;
		}
		
		// color things based on the index
		for( Token token : tokens )
		{
			try
			{
				m_editor.getHighlighter().addHighlight( token.start, token.end(), m_highlightPainter );
			}
			catch( BadLocationException ex )
			{
				throw new IllegalArgumentException( ex );
			}
		}
		
		redraw();
	}
	
	private void clearEntryPair( )
	{
		m_actionPanel.removeAll();
		JLabel label = new JLabel( "No identifier selected" );
		unboldLabel( label );
		label.setHorizontalAlignment( JLabel.CENTER );
		m_actionPanel.add( label );
		
		redraw();
	}
	
	@SuppressWarnings( "unchecked" )
	private void showEntryPair( EntryPair<Entry> pair )
	{
		if( pair == null )
		{
			clearEntryPair();
			return;
		}
		
		m_selectedEntryPair = pair;
		
		// layout the action panel
		m_actionPanel.removeAll();
		m_actionPanel.add( m_renamePanel );
		m_nameField.setText( pair.deobf.getName() );
		
		// layout the dynamic section
		JPanel dynamicPanel = new JPanel();
		dynamicPanel.setLayout( new GridLayout( 3, 1, 0, 0 ) );
		m_actionPanel.add( dynamicPanel );
		if( pair.deobf instanceof ClassEntry )
		{
			showClassEntryPair( (EntryPair<? extends ClassEntry>)pair, dynamicPanel );
		}
		else if( pair.deobf instanceof FieldEntry )
		{
			showFieldEntryPair( (EntryPair<? extends FieldEntry>)pair, dynamicPanel );
		}
		else if( pair.deobf instanceof MethodEntry )
		{
			showMethodEntryPair( (EntryPair<? extends MethodEntry>)pair, dynamicPanel );
		}
		else if( pair.deobf instanceof ArgumentEntry )
		{
			showArgumentEntryPair( (EntryPair<? extends ArgumentEntry>)pair, dynamicPanel );
		}
		else
		{
			throw new Error( "Unknown entry type: " + pair.deobf.getClass().getName() );
		}
		
		redraw();
	}
	
	private void showClassEntryPair( EntryPair<? extends ClassEntry> pair, JPanel panel )
	{
		m_typeLabel.setText( "Class: " );
	}
	
	private void showFieldEntryPair( EntryPair<? extends FieldEntry> pair, JPanel panel )
	{
		m_typeLabel.setText( "Field: " );
		addNameValue( panel, "Class", pair.obf.getClassEntry().getName() + " <-> " + pair.deobf.getClassEntry().getName() );
	}
	
	private void showMethodEntryPair( EntryPair<? extends MethodEntry> pair, JPanel panel )
	{
		m_typeLabel.setText( "Method: " );
		addNameValue( panel, "Class", pair.obf.getClassEntry().getName() + " <-> " + pair.deobf.getClassEntry().getName() );
		addNameValue( panel, "Signature", pair.obf.getSignature() + " <-> " + pair.deobf.getSignature() );
	}
	
	private void showArgumentEntryPair( EntryPair<? extends ArgumentEntry> pair, JPanel panel )
	{
		m_typeLabel.setText( "Argument: " );
		addNameValue( panel, "Class", pair.obf.getClassEntry().getName() + " <-> " + pair.deobf.getClassEntry().getName() );
		addNameValue( panel, "Method", pair.obf.getMethodEntry().getName() + " <-> " + pair.deobf.getMethodEntry().getName() );
		addNameValue( panel, "Index", Integer.toString( pair.obf.getIndex() ) );
	}
	
	private void addNameValue( JPanel container, String name, String value )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new FlowLayout( FlowLayout.LEFT, 6, 0 ) );
		container.add( panel );
		
		JLabel label = new JLabel( name + ":", JLabel.RIGHT );
		label.setPreferredSize( new Dimension( 80, label.getPreferredSize().height ) );
		panel.add( label );
		
		panel.add( unboldLabel( new JLabel( value, JLabel.LEFT ) ) );
	}

	private JLabel unboldLabel( JLabel label )
	{
		Font font = label.getFont();
		label.setFont( font.deriveFont( font.getStyle() & ~Font.BOLD ) );
		return label;
	}
	
	private void redraw( )
	{
		m_frame.validate();
		m_frame.repaint();
	}
}
