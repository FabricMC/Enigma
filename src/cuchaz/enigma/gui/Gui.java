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
import java.util.Map;
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
import jsyntaxpane.SyntaxDocument;

import com.google.common.collect.Lists;

import cuchaz.enigma.ClassFile;
import cuchaz.enigma.Constants;
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
	private static Comparator<ClassFile> m_obfuscatedClassSorter;
	private static Comparator<Map.Entry<ClassFile,String>> m_deobfuscatedClassSorter;
	
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
		
		m_deobfuscatedClassSorter = new Comparator<Map.Entry<ClassFile,String>>( )
		{
			@Override
			public int compare( Map.Entry<ClassFile,String> a, Map.Entry<ClassFile,String> b )
			{
				// I can never keep this rule straight when writing these damn things...
				// a < b => -1, a == b => 0, a > b => +1
				
				String[] aparts = a.getValue().split( "\\." );
				String[] bparts = b.getValue().split( "\\." );
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
	private JList<ClassFile> m_obfClasses;
	private JList<Map.Entry<ClassFile,String>> m_deobfClasses;
	private JEditorPane m_editor;
	private JPanel m_infoPanel;
	private BoxHighlightPainter m_obfuscatedHighlightPainter;
	private BoxHighlightPainter m_deobfuscatedHighlightPainter;
	private JTree m_inheritanceTree;
	
	// dynamic menu items
	private JMenuItem m_closeJarMenu;
	private JMenuItem m_openMappingsMenu;
	private JMenuItem m_saveMappingsMenu;
	private JMenuItem m_saveMappingsAsMenu;
	private JMenuItem m_closeMappingsMenu;
	private JMenuItem m_renameMenu;
	private JMenuItem m_inheritanceMenu;
	private JMenuItem m_openEntryMenu;
	
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
		m_obfClasses.setCellRenderer( new ObfuscatedClassListCellRenderer() );
		m_obfClasses.addMouseListener( new MouseAdapter()
		{
			@Override
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
		m_deobfClasses = new JList<Map.Entry<ClassFile,String>>();
		m_deobfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_deobfClasses.setLayoutOrientation( JList.VERTICAL );
		m_deobfClasses.setCellRenderer( new DeobfuscatedClassListCellRenderer() );
		m_deobfClasses.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					Map.Entry<ClassFile,String> selected = m_deobfClasses.getSelectedValue();
					if( selected != null )
					{
						m_controller.deobfuscateClass( selected.getKey() );
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
			popupMenu.add( menu );
			m_inheritanceMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem( "Open Class" );
			menu.addActionListener( new ActionListener( )
			{
				@Override
				public void actionPerformed( ActionEvent event )
				{
					openEntry();
				}
			} );
			popupMenu.add( menu );
			m_openEntryMenu = menu;
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
					ClassInheritanceTreeNode node = (ClassInheritanceTreeNode)m_inheritanceTree.getSelectionPath().getLastPathComponent();
					if( node != null )
					{
						m_controller.deobfuscateClass( new ClassFile( node.getObfClassName() ) );
					}
				}
			}
		} );
		JPanel inheritancePanel = new JPanel();
		inheritancePanel.setLayout( new BorderLayout() );
		inheritancePanel.add( new JScrollPane( m_inheritanceTree ) );
		
		// layout controls
		JSplitPane splitLeft = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true, obfPanel, deobfPanel );
		splitLeft.setPreferredSize( new Dimension( 200, 0 ) );
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout() );
		centerPanel.add( m_infoPanel, BorderLayout.NORTH );
		centerPanel.add( sourceScroller, BorderLayout.CENTER );
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setPreferredSize( new Dimension( 200, 0 ) );
		tabbedPane.addTab( "Inheritance", inheritancePanel );
		JSplitPane splitRight = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, tabbedPane );
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
	
	public void setObfClasses( List<ClassFile> obfClasses )
	{
		if( obfClasses != null )
		{
			Vector<ClassFile> sortedClasses = new Vector<ClassFile>( obfClasses );
			Collections.sort( sortedClasses, m_obfuscatedClassSorter );
			m_obfClasses.setListData( sortedClasses );
		}
		else
		{
			m_obfClasses.setListData( new Vector<ClassFile>() );
		}
	}
	
	public void setDeobfClasses( Map<ClassFile,String> deobfClasses )
	{
		if( deobfClasses != null )
		{
			Vector<Map.Entry<ClassFile,String>> sortedClasses = new Vector<Map.Entry<ClassFile,String>>( deobfClasses.entrySet() );
			Collections.sort( sortedClasses, m_deobfuscatedClassSorter );
			m_deobfClasses.setListData( sortedClasses );
		}
		else
		{
			m_deobfClasses.setListData( new Vector<Map.Entry<ClassFile,String>>() );
		}
	}
	
	public void setMappingsFile( File file )
	{
		m_mappingsFileChooser.setSelectedFile( file );
		m_saveMappingsMenu.setEnabled( file != null );
	}
	
	public void setSource( String source )
	{
		setSource( source, 0 );
	}
	
	public void setSource( String source, int lineNum )
	{
		// remove any old highlighters
		m_editor.getHighlighter().removeAllHighlights();
		
		m_editor.setText( source );
		
		// count the offset of the target line
		String text = m_editor.getText();
		int pos = 0;
		int numLines = 0;
		for( ; pos < text.length(); pos++ )
		{
			if( numLines == lineNum )
			{
				break;
			}
			if( text.charAt( pos ) == '\n' )
			{
				numLines++;
			}
		}
		
		// put the caret at the line number
		m_editor.setCaretPosition( pos );
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
		boolean isMethodEntry = m_selectedEntryPair.obf instanceof MethodEntry;
		
		showEntryPair( m_selectedEntryPair );
		
		m_inheritanceMenu.setEnabled( isClassEntry || isMethodEntry );
		m_openEntryMenu.setEnabled( isClassEntry );
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
			SyntaxDocument doc = (SyntaxDocument)m_editor.getDocument();
			int lineNum = doc.getLineNumberAt( m_editor.getCaretPosition() );
			try
			{
				// TODO: give token to the controller so we can put the caret back there
				m_controller.rename( m_selectedEntryPair.obf, newName, lineNum );
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
		
		// get the current class
		if( m_selectedEntryPair.obf instanceof ClassEntry )
		{
			ClassInheritanceTreeNode classNode = m_controller.getClassInheritance( (ClassEntry)m_selectedEntryPair.obf );
			
			// build the path from the root to the class node
			List<TreeNode> nodes = Lists.newArrayList();
			TreeNode node = classNode;
			do
			{
				nodes.add( node );
				node = node.getParent();
			}
			while( node != null );
			Collections.reverse( nodes );
			TreePath path = new TreePath( nodes.toArray() );
			
			// show the tree at the root
			m_inheritanceTree.setModel( new DefaultTreeModel( (TreeNode)path.getPathComponent( 0 ) ) );
			m_inheritanceTree.expandPath( path );
			m_inheritanceTree.setSelectionRow( m_inheritanceTree.getRowForPath( path ) );
		}
		redraw();
	}
	
	private void openEntry( )
	{	
		if( m_selectedEntryPair == null )
		{
			return;
		}
		
		// get the current class
		if( m_selectedEntryPair.obf instanceof ClassEntry )
		{
			m_controller.deobfuscateClass( new ClassFile( m_selectedEntryPair.obf.getName() ) );
		}
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
