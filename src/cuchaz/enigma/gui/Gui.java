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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import jsyntaxpane.DefaultSyntaxKit;

import com.google.common.collect.Lists;

import cuchaz.enigma.Constants;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.MethodCallsTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryPair;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.IllegalNameException;
import cuchaz.enigma.mapping.MethodEntry;

public class Gui
{
	private static Comparator<String> m_obfClassSorter;
	private static Comparator<String> m_deobfClassSorter;
	
	static
	{
		m_obfClassSorter = new Comparator<String>( )
		{
			@Override
			public int compare( String a, String b )
			{
				if( a.length() != b.length() )
				{
					return a.length() - b.length();
				}
				return a.compareTo( b );
			}
		};
		
		m_deobfClassSorter = new Comparator<String>( )
		{
			@Override
			public int compare( String a, String b )
			{
				// I can never keep this rule straight when writing these damn things...
				// a < b => -1, a == b => 0, a > b => +1
				
				String[] aparts = a.split( "\\." );
				String[] bparts = b.split( "\\." );
				for( int i=0; true; i++ )
				{
					if( i >= aparts.length )
					{
						return -1;
					}
					else if( i >= bparts.length )
					{
						return 1;
					}
					
					int result = aparts[i].compareTo( bparts[i] );
					if( result != 0 )
					{
						return result;
					}
				}
			}
		};
	}
	
	private GuiController m_controller;
	
	// controls
	private JFrame m_frame;
	private JList<String> m_obfClasses;
	private JList<String> m_deobfClasses;
	private JEditorPane m_editor;
	private JPanel m_infoPanel;
	private BoxHighlightPainter m_obfuscatedHighlightPainter;
	private BoxHighlightPainter m_deobfuscatedHighlightPainter;
	private JTree m_inheritanceTree;
	private JTree m_callsTree;
	private JTabbedPane m_tabs;
	
	// dynamic menu items
	private JMenuItem m_closeJarMenu;
	private JMenuItem m_openMappingsMenu;
	private JMenuItem m_saveMappingsMenu;
	private JMenuItem m_saveMappingsAsMenu;
	private JMenuItem m_closeMappingsMenu;
	private JMenuItem m_renameMenu;
	private JMenuItem m_showInheritanceMenu;
	private JMenuItem m_openEntryMenu;
	private JMenuItem m_openPreviousMenu;
	private JMenuItem m_showCallsMenu;
	
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
		m_obfClasses = new JList<String>();
		m_obfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_obfClasses.setLayoutOrientation( JList.VERTICAL );
		m_obfClasses.setCellRenderer( new ClassListCellRenderer() );
		m_obfClasses.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					String selected = m_obfClasses.getSelectedValue();
					if( selected != null )
					{
						m_controller.openEntry( new ClassEntry( selected ) );
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
		m_deobfClasses = new JList<String>();
		m_deobfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_deobfClasses.setLayoutOrientation( JList.VERTICAL );
		m_deobfClasses.setCellRenderer( new ClassListCellRenderer() );
		m_deobfClasses.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					String selected = m_deobfClasses.getSelectedValue();
					if( selected != null )
					{
						m_controller.openEntry( new ClassEntry( selected ) );
					}
				}
			}
		} );
		JScrollPane deobfScroller = new JScrollPane( m_deobfClasses );
		JPanel deobfPanel = new JPanel();
		deobfPanel.setLayout( new BorderLayout() );
		deobfPanel.add( new JLabel( "De-obfuscated Classes" ), BorderLayout.NORTH );
		deobfPanel.add( deobfScroller, BorderLayout.CENTER );
		
		// init info panel
		m_infoPanel = new JPanel();
		m_infoPanel.setLayout( new GridLayout( 4, 1, 0, 0 ) );
		m_infoPanel.setPreferredSize( new Dimension( 0, 100 ) );
		m_infoPanel.setBorder( BorderFactory.createTitledBorder( "Identifier Info" ) );
		clearEntryPair();
		
		// init editor
		DefaultSyntaxKit.initKit();
		m_obfuscatedHighlightPainter = new ObfuscatedHighlightPainter();
		m_deobfuscatedHighlightPainter = new DeobfuscatedHighlightPainter();
		m_editor = new JEditorPane();
		m_editor.setEditable( false );
		m_editor.setCaret( new BrowserCaret() );
		JScrollPane sourceScroller = new JScrollPane( m_editor );
		m_editor.setContentType( "text/java" );
		m_editor.addCaretListener( new CaretListener( )
		{
			@Override
			public void caretUpdate( CaretEvent event )
			{
				onCaretMove( event.getDot() );
			}
		} );
		m_editor.addKeyListener( new KeyAdapter( )
		{
			@Override
			public void keyPressed( KeyEvent event )
			{
				switch( event.getKeyCode() )
				{
					case KeyEvent.VK_R:
						startRename();
					break;
					
					case KeyEvent.VK_I:
						showInheritance();
					break;
					
					case KeyEvent.VK_N:
						openEntry();
					break;
					
					case KeyEvent.VK_P:
						m_controller.openPreviousEntry();
					break;
					
					case KeyEvent.VK_C:
						showCalls();
					break;
				}
			}
		} );
		
		// turn off token highlighting (it's wrong most of the time anyway...)
		DefaultSyntaxKit kit = (DefaultSyntaxKit)m_editor.getEditorKit();
		kit.toggleComponent( m_editor, "jsyntaxpane.components.TokenMarker" );
		
		// init editor popup menu
		JPopupMenu popupMenu = new JPopupMenu();
		m_editor.setComponentPopupMenu( popupMenu );
		{
			JMenuItem menu = new JMenuItem( "Rename" );
			menu.addActionListener( new ActionListener( )
			{
				@Override
				public void actionPerformed( ActionEvent event )
				{
					startRename();
				}
			} );
			menu.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 ) );
			menu.setEnabled( false );
			popupMenu.add( menu );
			m_renameMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem( "Show Inheritance" );
			menu.addActionListener( new ActionListener( )
			{
				@Override
				public void actionPerformed( ActionEvent event )
				{
					showInheritance();
				}
			} );
			menu.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_I, 0 ) );
			menu.setEnabled( false );
			popupMenu.add( menu );
			m_showInheritanceMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem( "Show Calls" );
			menu.addActionListener( new ActionListener( )
			{
				@Override
				public void actionPerformed( ActionEvent event )
				{
					showCalls();
				}
			} );
			menu.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 ) );
			menu.setEnabled( false );
			popupMenu.add( menu );
			m_showCallsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem( "Go to Declaration" );
			menu.addActionListener( new ActionListener( )
			{
				@Override
				public void actionPerformed( ActionEvent event )
				{
					openEntry();
				}
			} );
			menu.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_N, 0 ) );
			menu.setEnabled( false );
			popupMenu.add( menu );
			m_openEntryMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem( "Go to previous" );
			menu.addActionListener( new ActionListener( )
			{
				@Override
				public void actionPerformed( ActionEvent event )
				{
					m_controller.openPreviousEntry();
				}
			} );
			menu.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_P, 0 ) );
			menu.setEnabled( false );
			popupMenu.add( menu );
			m_openPreviousMenu = menu;
		}
		
		// init inheritance panel
		m_inheritanceTree = new JTree();
		m_inheritanceTree.setModel( null );
		m_inheritanceTree.addMouseListener( new MouseAdapter( )
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					// get the selected node
					TreePath path = m_inheritanceTree.getSelectionPath();
					if( path == null )
					{
						return;
					}
					
					Object node = path.getLastPathComponent();
					if( node instanceof ClassInheritanceTreeNode )
					{
						m_controller.openEntry( new ClassEntry( ((ClassInheritanceTreeNode)node).getObfClassName() ) );
					}
					else if( node instanceof MethodInheritanceTreeNode )
					{
						MethodInheritanceTreeNode methodNode = (MethodInheritanceTreeNode)node;
						if( methodNode.isImplemented() )
						{
							m_controller.openEntry( methodNode.getMethodEntry() );
						}
					}
				}
			}
		} );
		JPanel inheritancePanel = new JPanel();
		inheritancePanel.setLayout( new BorderLayout() );
		inheritancePanel.add( new JScrollPane( m_inheritanceTree ) );
		
		// init call panel
		m_callsTree = new JTree();
		m_callsTree.setModel( null );
		m_callsTree.addMouseListener( new MouseAdapter( )
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					// get the selected node
					TreePath path = m_callsTree.getSelectionPath();
					if( path == null )
					{
						return;
					}
					
					Object node = path.getLastPathComponent();
					if( node instanceof MethodCallsTreeNode )
					{
						m_controller.openEntry( ((MethodCallsTreeNode)node).getMethodEntry() );
					}
				}
			}
		} );
		JPanel callPanel = new JPanel();
		callPanel.setLayout( new BorderLayout() );
		callPanel.add( new JScrollPane( m_callsTree ) );
		
		// layout controls
		JSplitPane splitLeft = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true, obfPanel, deobfPanel );
		splitLeft.setPreferredSize( new Dimension( 250, 0 ) );
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout() );
		centerPanel.add( m_infoPanel, BorderLayout.NORTH );
		centerPanel.add( sourceScroller, BorderLayout.CENTER );
		m_tabs = new JTabbedPane();
		m_tabs.setPreferredSize( new Dimension( 250, 0 ) );
		m_tabs.addTab( "Inheritance", inheritancePanel );
		m_tabs.addTab( "Method Calls", callPanel );
		JSplitPane splitRight = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, m_tabs );
		splitRight.setResizeWeight( 1 ); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		JSplitPane splitCenter = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, splitRight );
		splitCenter.setResizeWeight( 0 ); // let the right side take all the slack
		pane.add( splitCenter, BorderLayout.CENTER );
		
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
				item.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK ) );
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
				item.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ) );
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
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem( "Exit" );
				menu.add( item );
				item.addActionListener( new ActionListener( )
				{
					@Override
					public void actionPerformed( ActionEvent event )
					{
						close();
					}
				} );
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
		
		m_frame.addWindowListener( new WindowAdapter( )
		{
			@Override
			public void windowClosing( WindowEvent event )
			{
				close();
			}
		} );
		
		// show the frame
		pane.doLayout();
		m_frame.setSize( 1024, 576 );
		m_frame.setMinimumSize( new Dimension( 640, 480 ) );
		m_frame.setVisible( true );
		m_frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
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
		setDeobfClasses( null );
		setSource( null );
		
		// update menu
		m_closeJarMenu.setEnabled( false );
		m_openMappingsMenu.setEnabled( false );
		m_saveMappingsMenu.setEnabled( false );
		m_saveMappingsAsMenu.setEnabled( false );
		m_closeMappingsMenu.setEnabled( false );
	}
	
	public void setObfClasses( List<String> obfClasses )
	{
		if( obfClasses != null )
		{
			Vector<String> sortedClasses = new Vector<String>( obfClasses );
			Collections.sort( sortedClasses, m_obfClassSorter );
			m_obfClasses.setListData( sortedClasses );
		}
		else
		{
			m_obfClasses.setListData( new Vector<String>() );
		}
	}
	
	public void setDeobfClasses( List<String> deobfClasses )
	{
		if( deobfClasses != null )
		{
			Vector<String> sortedClasses = new Vector<String>( deobfClasses );
			Collections.sort( sortedClasses, m_deobfClassSorter );
			m_deobfClasses.setListData( sortedClasses );
		}
		else
		{
			m_deobfClasses.setListData( new Vector<String>() );
		}
	}
	
	public void setMappingsFile( File file )
	{
		m_mappingsFileChooser.setSelectedFile( file );
		m_saveMappingsMenu.setEnabled( file != null );
	}
	
	public void setSource( String source )
	{
		m_editor.getHighlighter().removeAllHighlights();
		m_editor.setText( source );
	}
	
	public void showToken( Token token )
	{
		m_editor.setCaretPosition( token.start );
		m_editor.grabFocus();
	}
	
	public void setHighlightedTokens( Iterable<Token> obfuscatedTokens, Iterable<Token> deobfuscatedTokens )
	{
		// remove any old highlighters
		m_editor.getHighlighter().removeAllHighlights();
		
		// color things based on the index
		if( obfuscatedTokens != null )
		{
			setHighlightedTokens( obfuscatedTokens, m_obfuscatedHighlightPainter );
		}
		if( deobfuscatedTokens != null )
		{
			setHighlightedTokens( deobfuscatedTokens, m_deobfuscatedHighlightPainter );
		}
		
		redraw();
	}
	
	private void setHighlightedTokens( Iterable<Token> tokens, Highlighter.HighlightPainter painter )
	{
		for( Token token : tokens )
		{
			try
			{
				m_editor.getHighlighter().addHighlight( token.start, token.end, painter );
			}
			catch( BadLocationException ex )
			{
				throw new IllegalArgumentException( ex );
			}
		}
	}
	
	private void clearEntryPair( )
	{
		m_infoPanel.removeAll();
		JLabel label = new JLabel( "No identifier selected" );
		unboldLabel( label );
		label.setHorizontalAlignment( JLabel.CENTER );
		m_infoPanel.add( label );
		
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
		
		m_infoPanel.removeAll();
		if( pair.deobf instanceof ClassEntry )
		{
			showClassEntryPair( (EntryPair<? extends ClassEntry>)pair );
		}
		else if( pair.deobf instanceof FieldEntry )
		{
			showFieldEntryPair( (EntryPair<? extends FieldEntry>)pair );
		}
		else if( pair.deobf instanceof MethodEntry )
		{
			showMethodEntryPair( (EntryPair<? extends MethodEntry>)pair );
		}
		else if( pair.deobf instanceof ArgumentEntry )
		{
			showArgumentEntryPair( (EntryPair<? extends ArgumentEntry>)pair );
		}
		else
		{
			throw new Error( "Unknown entry type: " + pair.deobf.getClass().getName() );
		}
		
		redraw();
	}
	
	private void showClassEntryPair( EntryPair<? extends ClassEntry> pair )
	{
		addNameValue( m_infoPanel, "Class", pair.deobf.getName() );
	}
	
	private void showFieldEntryPair( EntryPair<? extends FieldEntry> pair )
	{
		addNameValue( m_infoPanel, "Field", pair.deobf.getName() );
		addNameValue( m_infoPanel, "Class", pair.deobf.getClassEntry().getName() );
	}
	
	private void showMethodEntryPair( EntryPair<? extends MethodEntry> pair )
	{
		addNameValue( m_infoPanel, "Method", pair.deobf.getName() );
		addNameValue( m_infoPanel, "Class", pair.deobf.getClassEntry().getName() );
		addNameValue( m_infoPanel, "Signature", pair.deobf.getSignature() );
	}
	
	private void showArgumentEntryPair( EntryPair<? extends ArgumentEntry> pair )
	{
		addNameValue( m_infoPanel, "Argument", pair.deobf.getName() );
		addNameValue( m_infoPanel, "Class", pair.deobf.getClassEntry().getName() );
		addNameValue( m_infoPanel, "Method", pair.deobf.getMethodEntry().getName() );
		addNameValue( m_infoPanel, "Index", Integer.toString( pair.deobf.getIndex() ) );
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
	
	private void onCaretMove( int pos )
	{
		Token token = m_controller.getToken( pos );
		m_renameMenu.setEnabled( token != null );
		if( token == null )
		{
			clearEntryPair();
			return;
		}
		
		m_selectedEntryPair = m_controller.getEntryPair( token );
		boolean isClassEntry = m_selectedEntryPair.obf instanceof ClassEntry;
		boolean isFieldEntry = m_selectedEntryPair.obf instanceof FieldEntry;
		boolean isMethodEntry = m_selectedEntryPair.obf instanceof MethodEntry;
		
		showEntryPair( m_selectedEntryPair );
		
		m_showInheritanceMenu.setEnabled( isClassEntry || isMethodEntry );
		m_showCallsMenu.setEnabled( isMethodEntry );
		m_openEntryMenu.setEnabled( isClassEntry || isFieldEntry || isMethodEntry );
		m_openPreviousMenu.setEnabled( m_controller.hasPreviousEntry() );
	}
	
	private void startRename( )
	{
		// init the text box
		final JTextField text = new JTextField();
		text.setText( m_selectedEntryPair.deobf.getName() );
		text.setPreferredSize( new Dimension( 360, text.getPreferredSize().height ) );
		text.addKeyListener( new KeyAdapter( )
		{
			@Override
			public void keyPressed( KeyEvent event )
			{
				switch( event.getKeyCode() )
				{
					case KeyEvent.VK_ENTER:
						finishRename( text, true );
					break;
					
					case KeyEvent.VK_ESCAPE:
						finishRename( text, false );
					break;
				}
			}
		} );
		
		// find the label with the name and replace it with the text box
		JPanel panel = (JPanel)m_infoPanel.getComponent( 0 );
		panel.remove( panel.getComponentCount() - 1 );
		panel.add( text );
		text.grabFocus();
		text.selectAll();
		
		redraw();
	}
	
	private void finishRename( JTextField text, boolean saveName )
	{
		String newName = text.getText();
		if( saveName && newName != null && newName.length() > 0 )
		{
			try
			{
				m_controller.rename( m_selectedEntryPair.obf, newName );
			}
			catch( IllegalNameException ex )
			{
				text.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
			}
			return;
		}
		
		// abort the rename
		JPanel panel = (JPanel)m_infoPanel.getComponent( 0 );
		panel.remove( panel.getComponentCount() - 1 );
		panel.add( unboldLabel( new JLabel( m_selectedEntryPair.deobf.getName(), JLabel.LEFT ) ) );
		
		m_editor.grabFocus();
		
		redraw();
	}
	
	private void showInheritance( )
	{
		if( m_selectedEntryPair == null )
		{
			return;
		}
		
		if( m_selectedEntryPair.obf instanceof ClassEntry )
		{
			// get the class inheritance
			ClassInheritanceTreeNode classNode = m_controller.getClassInheritance( (ClassEntry)m_selectedEntryPair.obf );
			
			// show the tree at the root
			TreePath path = getPathToRoot( classNode );
			m_inheritanceTree.setModel( new DefaultTreeModel( (TreeNode)path.getPathComponent( 0 ) ) );
			m_inheritanceTree.expandPath( path );
			m_inheritanceTree.setSelectionRow( m_inheritanceTree.getRowForPath( path ) );
		}
		else if( m_selectedEntryPair.obf instanceof MethodEntry )
		{
			// get the method inheritance
			MethodInheritanceTreeNode classNode = m_controller.getMethodInheritance( (MethodEntry)m_selectedEntryPair.obf );
			
			// show the tree at the root
			TreePath path = getPathToRoot( classNode );
			m_inheritanceTree.setModel( new DefaultTreeModel( (TreeNode)path.getPathComponent( 0 ) ) );
			m_inheritanceTree.expandPath( path );
			m_inheritanceTree.setSelectionRow( m_inheritanceTree.getRowForPath( path ) );
		}
		
		m_tabs.setSelectedIndex( 0 );
		redraw();
	}
	
	private void showCalls( )
	{
		if( m_selectedEntryPair == null )
		{
			return;
		}
		
		if( m_selectedEntryPair.obf instanceof MethodEntry )
		{
			MethodCallsTreeNode node = m_controller.getMethodCalls( (MethodEntry)m_selectedEntryPair.obf );
			m_callsTree.setModel( new DefaultTreeModel( node ) );
		}
		
		m_tabs.setSelectedIndex( 1 );
		redraw();
	}
	
	private TreePath getPathToRoot( TreeNode node )
	{
		List<TreeNode> nodes = Lists.newArrayList();
		TreeNode n = node;
		do
		{
			nodes.add( n );
			n = n.getParent();
		}
		while( n != null );
		Collections.reverse( nodes );
		return new TreePath( nodes.toArray() );
	}
	
	private void openEntry( )
	{	
		if( m_selectedEntryPair == null )
		{
			return;
		}
		m_controller.openEntry( m_selectedEntryPair.obf );
	}
	
	private void close( )
	{
		if( !m_controller.isDirty() )
		{
			// everything is saved, we can exit safely
			m_frame.dispose();
		}
		else
		{
			// ask to save before closing
			String[] options = {
				"Save and exit",
				"Discard changes",
				"Cancel"
			};
			int response = JOptionPane.showOptionDialog(
				m_frame,
				"Your mappings have not been saved yet. Do you want to save?",
				"Save your changes?",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[2]
			);
			switch( response )
			{
				case JOptionPane.YES_OPTION: // save and exit
					if( m_mappingsFileChooser.getSelectedFile() != null || m_mappingsFileChooser.showSaveDialog( m_frame ) == JFileChooser.APPROVE_OPTION )
					{
						try
						{
							m_controller.saveMappings( m_mappingsFileChooser.getSelectedFile() );
							m_frame.dispose();
						}
						catch( IOException ex )
						{
							throw new Error( ex );
						}
					}
				break;
				
				case JOptionPane.NO_OPTION:
					// don't save, exit
					m_frame.dispose();
				break;
				
				// cancel means do nothing
			}
		}
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
