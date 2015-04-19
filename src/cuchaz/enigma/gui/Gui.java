/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarFile;

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

import com.google.common.collect.Lists;

import cuchaz.enigma.Constants;
import cuchaz.enigma.ExceptionIgnorer;
import cuchaz.enigma.analysis.BehaviorReferenceTreeNode;
import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.FieldReferenceTreeNode;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.ReferenceTreeNode;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.gui.ClassSelector.ClassSelectionListener;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.IllegalNameException;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Signature;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class Gui {
	
	private GuiController m_controller;
	
	// controls
	private JFrame m_frame;
	private ClassSelector m_obfClasses;
	private ClassSelector m_deobfClasses;
	private JEditorPane m_editor;
	private JPanel m_classesPanel;
	private JSplitPane m_splitClasses;
	private JPanel m_infoPanel;
	private ObfuscatedHighlightPainter m_obfuscatedHighlightPainter;
	private DeobfuscatedHighlightPainter m_deobfuscatedHighlightPainter;
	private OtherHighlightPainter m_otherHighlightPainter;
	private SelectionHighlightPainter m_selectionHighlightPainter;
	private JTree m_inheritanceTree;
	private JTree m_implementationsTree;
	private JTree m_callsTree;
	private JList<Token> m_tokens;
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
	private JMenuItem m_showImplementationsMenu;
	private JMenuItem m_toggleMappingMenu;
	private JMenuItem m_exportSourceMenu;
	private JMenuItem m_exportJarMenu;
	
	// state
	private EntryReference<Entry,Entry> m_reference;
	private JFileChooser m_jarFileChooser;
	private JFileChooser m_mappingsFileChooser;
	private JFileChooser m_exportSourceFileChooser;
	private JFileChooser m_exportJarFileChooser;
	
	public Gui() {
		
		// init frame
		m_frame = new JFrame(Constants.Name);
		final Container pane = m_frame.getContentPane();
		pane.setLayout(new BorderLayout());
		
		if (Boolean.parseBoolean(System.getProperty("enigma.catchExceptions", "true"))) {
			// install a global exception handler to the event thread
			CrashDialog.init(m_frame);
			Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable t) {
					t.printStackTrace(System.err);
					if (!ExceptionIgnorer.shouldIgnore(t)) {
						CrashDialog.show(t);
					}
				}
			});
		}
		
		m_controller = new GuiController(this);
		
		// init file choosers
		m_jarFileChooser = new JFileChooser();
		m_mappingsFileChooser = new JFileChooser();
		m_exportSourceFileChooser = new JFileChooser();
		m_exportSourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		m_exportJarFileChooser = new JFileChooser();
		
		// init obfuscated classes list
		m_obfClasses = new ClassSelector(ClassSelector.ObfuscatedClassEntryComparator);
		m_obfClasses.setListener(new ClassSelectionListener() {
			@Override
			public void onSelectClass(ClassEntry classEntry) {
				navigateTo(classEntry);
			}
		});
		JScrollPane obfScroller = new JScrollPane(m_obfClasses);
		JPanel obfPanel = new JPanel();
		obfPanel.setLayout(new BorderLayout());
		obfPanel.add(new JLabel("Obfuscated Classes"), BorderLayout.NORTH);
		obfPanel.add(obfScroller, BorderLayout.CENTER);
		
		// init deobfuscated classes list
		m_deobfClasses = new ClassSelector(ClassSelector.DeobfuscatedClassEntryComparator);
		m_deobfClasses.setListener(new ClassSelectionListener() {
			@Override
			public void onSelectClass(ClassEntry classEntry) {
				navigateTo(classEntry);
			}
		});
		JScrollPane deobfScroller = new JScrollPane(m_deobfClasses);
		JPanel deobfPanel = new JPanel();
		deobfPanel.setLayout(new BorderLayout());
		deobfPanel.add(new JLabel("De-obfuscated Classes"), BorderLayout.NORTH);
		deobfPanel.add(deobfScroller, BorderLayout.CENTER);
		
		// set up classes panel (don't add the splitter yet)
		m_splitClasses = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, obfPanel, deobfPanel);
		m_splitClasses.setResizeWeight(0.3);
		m_classesPanel = new JPanel();
		m_classesPanel.setLayout(new BorderLayout());
		m_classesPanel.setPreferredSize(new Dimension(250, 0));
		
		// init info panel
		m_infoPanel = new JPanel();
		m_infoPanel.setLayout(new GridLayout(4, 1, 0, 0));
		m_infoPanel.setPreferredSize(new Dimension(0, 100));
		m_infoPanel.setBorder(BorderFactory.createTitledBorder("Identifier Info"));
		clearReference();
		
		// init editor
		DefaultSyntaxKit.initKit();
		m_obfuscatedHighlightPainter = new ObfuscatedHighlightPainter();
		m_deobfuscatedHighlightPainter = new DeobfuscatedHighlightPainter();
		m_otherHighlightPainter = new OtherHighlightPainter();
		m_selectionHighlightPainter = new SelectionHighlightPainter();
		m_editor = new JEditorPane();
		m_editor.setEditable(false);
		m_editor.setCaret(new BrowserCaret());
		JScrollPane sourceScroller = new JScrollPane(m_editor);
		m_editor.setContentType("text/java");
		m_editor.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent event) {
				onCaretMove(event.getDot());
			}
		});
		m_editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_R:
						m_renameMenu.doClick();
					break;
					
					case KeyEvent.VK_I:
						m_showInheritanceMenu.doClick();
					break;
					
					case KeyEvent.VK_M:
						m_showImplementationsMenu.doClick();
					break;
					
					case KeyEvent.VK_N:
						m_openEntryMenu.doClick();
					break;
					
					case KeyEvent.VK_P:
						m_openPreviousMenu.doClick();
					break;
					
					case KeyEvent.VK_C:
						m_showCallsMenu.doClick();
					break;
					
					case KeyEvent.VK_T:
						m_toggleMappingMenu.doClick();
					break;
				}
			}
		});
		
		// turn off token highlighting (it's wrong most of the time anyway...)
		DefaultSyntaxKit kit = (DefaultSyntaxKit)m_editor.getEditorKit();
		kit.toggleComponent(m_editor, "de.sciss.syntaxpane.components.TokenMarker");
		
		// init editor popup menu
		JPopupMenu popupMenu = new JPopupMenu();
		m_editor.setComponentPopupMenu(popupMenu);
		{
			JMenuItem menu = new JMenuItem("Rename");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					startRename();
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_renameMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Inheritance");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					showInheritance();
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_showInheritanceMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Implementations");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					showImplementations();
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_showImplementationsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Calls");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					showCalls();
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_showCallsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Go to Declaration");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					navigateTo(m_reference.entry);
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_openEntryMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Go to previous");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					m_controller.openPreviousReference();
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_openPreviousMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Mark as deobfuscated");
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					toggleMapping();
				}
			});
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
			menu.setEnabled(false);
			popupMenu.add(menu);
			m_toggleMappingMenu = menu;
		}
		
		// init inheritance panel
		m_inheritanceTree = new JTree();
		m_inheritanceTree.setModel(null);
		m_inheritanceTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					// get the selected node
					TreePath path = m_inheritanceTree.getSelectionPath();
					if (path == null) {
						return;
					}
					
					Object node = path.getLastPathComponent();
					if (node instanceof ClassInheritanceTreeNode) {
						ClassInheritanceTreeNode classNode = (ClassInheritanceTreeNode)node;
						navigateTo(new ClassEntry(classNode.getObfClassName()));
					} else if (node instanceof MethodInheritanceTreeNode) {
						MethodInheritanceTreeNode methodNode = (MethodInheritanceTreeNode)node;
						if (methodNode.isImplemented()) {
							navigateTo(methodNode.getMethodEntry());
						}
					}
				}
			}
		});
		JPanel inheritancePanel = new JPanel();
		inheritancePanel.setLayout(new BorderLayout());
		inheritancePanel.add(new JScrollPane(m_inheritanceTree));
		
		// init implementations panel
		m_implementationsTree = new JTree();
		m_implementationsTree.setModel(null);
		m_implementationsTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					// get the selected node
					TreePath path = m_implementationsTree.getSelectionPath();
					if (path == null) {
						return;
					}
					
					Object node = path.getLastPathComponent();
					if (node instanceof ClassImplementationsTreeNode) {
						ClassImplementationsTreeNode classNode = (ClassImplementationsTreeNode)node;
						navigateTo(classNode.getClassEntry());
					} else if (node instanceof MethodImplementationsTreeNode) {
						MethodImplementationsTreeNode methodNode = (MethodImplementationsTreeNode)node;
						navigateTo(methodNode.getMethodEntry());
					}
				}
			}
		});
		JPanel implementationsPanel = new JPanel();
		implementationsPanel.setLayout(new BorderLayout());
		implementationsPanel.add(new JScrollPane(m_implementationsTree));
		
		// init call panel
		m_callsTree = new JTree();
		m_callsTree.setModel(null);
		m_callsTree.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					// get the selected node
					TreePath path = m_callsTree.getSelectionPath();
					if (path == null) {
						return;
					}
					
					Object node = path.getLastPathComponent();
					if (node instanceof ReferenceTreeNode) {
						ReferenceTreeNode<Entry,Entry> referenceNode = ((ReferenceTreeNode<Entry,Entry>)node);
						if (referenceNode.getReference() != null) {
							navigateTo(referenceNode.getReference());
						} else {
							navigateTo(referenceNode.getEntry());
						}
					}
				}
			}
		});
		m_tokens = new JList<Token>();
		m_tokens.setCellRenderer(new TokenListCellRenderer(m_controller));
		m_tokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tokens.setLayoutOrientation(JList.VERTICAL);
		m_tokens.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					Token selected = m_tokens.getSelectedValue();
					if (selected != null) {
						showToken(selected);
					}
				}
			}
		});
		m_tokens.setPreferredSize(new Dimension(0, 200));
		m_tokens.setMinimumSize(new Dimension(0, 200));
		JSplitPane callPanel = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			true,
			new JScrollPane(m_callsTree),
			new JScrollPane(m_tokens)
		);
		callPanel.setResizeWeight(1); // let the top side take all the slack
		callPanel.resetToPreferredSizes();
		
		// layout controls
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(m_infoPanel, BorderLayout.NORTH);
		centerPanel.add(sourceScroller, BorderLayout.CENTER);
		m_tabs = new JTabbedPane();
		m_tabs.setPreferredSize(new Dimension(250, 0));
		m_tabs.addTab("Inheritance", inheritancePanel);
		m_tabs.addTab("Implementations", implementationsPanel);
		m_tabs.addTab("Call Graph", callPanel);
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, m_tabs);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, m_classesPanel, splitRight);
		splitCenter.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitCenter, BorderLayout.CENTER);
		
		// init menus
		JMenuBar menuBar = new JMenuBar();
		m_frame.setJMenuBar(menuBar);
		{
			JMenu menu = new JMenu("File");
			menuBar.add(menu);
			{
				JMenuItem item = new JMenuItem("Open Jar...");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (m_jarFileChooser.showOpenDialog(m_frame) == JFileChooser.APPROVE_OPTION) {
							// load the jar in a separate thread
							new Thread() {
								@Override
								public void run() {
									try {
										m_controller.openJar(new JarFile(m_jarFileChooser.getSelectedFile()));
									} catch (IOException ex) {
										throw new Error(ex);
									}
								}
							}.start();
						}
					}
				});
			}
			{
				JMenuItem item = new JMenuItem("Close Jar");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						m_controller.closeJar();
					}
				});
				m_closeJarMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem("Open Mappings...");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (m_mappingsFileChooser.showOpenDialog(m_frame) == JFileChooser.APPROVE_OPTION) {
							try {
								m_controller.openMappings(m_mappingsFileChooser.getSelectedFile());
							} catch (IOException ex) {
								throw new Error(ex);
							} catch (MappingParseException ex) {
								JOptionPane.showMessageDialog(m_frame, ex.getMessage());
							}
						}
					}
				});
				m_openMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Save Mappings");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						try {
							m_controller.saveMappings(m_mappingsFileChooser.getSelectedFile());
						} catch (IOException ex) {
							throw new Error(ex);
						}
					}
				});
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
				m_saveMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Save Mappings As...");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (m_mappingsFileChooser.showSaveDialog(m_frame) == JFileChooser.APPROVE_OPTION) {
							try {
								m_controller.saveMappings(m_mappingsFileChooser.getSelectedFile());
								m_saveMappingsMenu.setEnabled(true);
							} catch (IOException ex) {
								throw new Error(ex);
							}
						}
					}
				});
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
				m_saveMappingsAsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Close Mappings");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						m_controller.closeMappings();
					}
				});
				m_closeMappingsMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem("Export Source...");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (m_exportSourceFileChooser.showSaveDialog(m_frame) == JFileChooser.APPROVE_OPTION) {
							m_controller.exportSource(m_exportSourceFileChooser.getSelectedFile());
						}
					}
				});
				m_exportSourceMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Export Jar...");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (m_exportJarFileChooser.showSaveDialog(m_frame) == JFileChooser.APPROVE_OPTION) {
							m_controller.exportJar(m_exportJarFileChooser.getSelectedFile());
						}
					}
				});
				m_exportJarMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem("Exit");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						close();
					}
				});
			}
		}
		{
			JMenu menu = new JMenu("Help");
			menuBar.add(menu);
			{
				JMenuItem item = new JMenuItem("About");
				menu.add(item);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						AboutDialog.show(m_frame);
					}
				});
			}
		}
		
		// init state
		onCloseJar();
		
		m_frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				close();
			}
		});
		
		// show the frame
		pane.doLayout();
		m_frame.setSize(1024, 576);
		m_frame.setMinimumSize(new Dimension(640, 480));
		m_frame.setVisible(true);
		m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}
	
	public JFrame getFrame() {
		return m_frame;
	}
	
	public GuiController getController() {
		return m_controller;
	}
	
	public void onStartOpenJar() {
		m_classesPanel.removeAll();
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		panel.add(new JLabel("Loading..."));
		m_classesPanel.add(panel);
		redraw();
	}
	
	public void onFinishOpenJar(String jarName) {
		// update gui
		m_frame.setTitle(Constants.Name + " - " + jarName);
		m_classesPanel.removeAll();
		m_classesPanel.add(m_splitClasses);
		setSource(null);
		
		// update menu
		m_closeJarMenu.setEnabled(true);
		m_openMappingsMenu.setEnabled(true);
		m_saveMappingsMenu.setEnabled(false);
		m_saveMappingsAsMenu.setEnabled(true);
		m_closeMappingsMenu.setEnabled(true);
		m_exportSourceMenu.setEnabled(true);
		m_exportJarMenu.setEnabled(true);
		
		redraw();
	}
	
	public void onCloseJar() {
		// update gui
		m_frame.setTitle(Constants.Name);
		setObfClasses(null);
		setDeobfClasses(null);
		setSource(null);
		m_classesPanel.removeAll();
		
		// update menu
		m_closeJarMenu.setEnabled(false);
		m_openMappingsMenu.setEnabled(false);
		m_saveMappingsMenu.setEnabled(false);
		m_saveMappingsAsMenu.setEnabled(false);
		m_closeMappingsMenu.setEnabled(false);
		m_exportSourceMenu.setEnabled(false);
		m_exportJarMenu.setEnabled(false);
		
		redraw();
	}
	
	public void setObfClasses(Collection<ClassEntry> obfClasses) {
		m_obfClasses.setClasses(obfClasses);
	}
	
	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		m_deobfClasses.setClasses(deobfClasses);
	}
	
	public void setMappingsFile(File file) {
		m_mappingsFileChooser.setSelectedFile(file);
		m_saveMappingsMenu.setEnabled(file != null);
	}
	
	public void setSource(String source) {
		m_editor.getHighlighter().removeAllHighlights();
		m_editor.setText(source);
	}
	
	public void showToken(final Token token) {
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null!");
		}
		CodeReader.navigateToToken(m_editor, token, m_selectionHighlightPainter);
		redraw();
	}
	
	public void showTokens(Collection<Token> tokens) {
		Vector<Token> sortedTokens = new Vector<Token>(tokens);
		Collections.sort(sortedTokens);
		if (sortedTokens.size() > 1) {
			// sort the tokens and update the tokens panel
			m_tokens.setListData(sortedTokens);
			m_tokens.setSelectedIndex(0);
		} else {
			m_tokens.setListData(new Vector<Token>());
		}
		
		// show the first token
		showToken(sortedTokens.get(0));
	}
	
	public void setHighlightedTokens(Iterable<Token> obfuscatedTokens, Iterable<Token> deobfuscatedTokens, Iterable<Token> otherTokens) {
		
		// remove any old highlighters
		m_editor.getHighlighter().removeAllHighlights();
		
		// color things based on the index
		if (obfuscatedTokens != null) {
			setHighlightedTokens(obfuscatedTokens, m_obfuscatedHighlightPainter);
		}
		if (deobfuscatedTokens != null) {
			setHighlightedTokens(deobfuscatedTokens, m_deobfuscatedHighlightPainter);
		}
		if (otherTokens != null) {
			setHighlightedTokens(otherTokens, m_otherHighlightPainter);
		}
		
		redraw();
	}
	
	private void setHighlightedTokens(Iterable<Token> tokens, Highlighter.HighlightPainter painter) {
		for (Token token : tokens) {
			try {
				m_editor.getHighlighter().addHighlight(token.start, token.end, painter);
			} catch (BadLocationException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}
	
	private void clearReference() {
		m_infoPanel.removeAll();
		JLabel label = new JLabel("No identifier selected");
		GuiTricks.unboldLabel(label);
		label.setHorizontalAlignment(JLabel.CENTER);
		m_infoPanel.add(label);
		
		redraw();
	}
	
	private void showReference(EntryReference<Entry,Entry> reference) {
		if (reference == null) {
			clearReference();
			return;
		}
		
		m_reference = reference;
		
		m_infoPanel.removeAll();
		if (reference.entry instanceof ClassEntry) {
			showClassEntry((ClassEntry)m_reference.entry);
		} else if (m_reference.entry instanceof FieldEntry) {
			showFieldEntry((FieldEntry)m_reference.entry);
		} else if (m_reference.entry instanceof MethodEntry) {
			showMethodEntry((MethodEntry)m_reference.entry);
		} else if (m_reference.entry instanceof ConstructorEntry) {
			showConstructorEntry((ConstructorEntry)m_reference.entry);
		} else if (m_reference.entry instanceof ArgumentEntry) {
			showArgumentEntry((ArgumentEntry)m_reference.entry);
		} else {
			throw new Error("Unknown entry type: " + m_reference.entry.getClass().getName());
		}
		
		redraw();
	}
	
	private void showClassEntry(ClassEntry entry) {
		addNameValue(m_infoPanel, "Class", entry.getName());
	}
	
	private void showFieldEntry(FieldEntry entry) {
		addNameValue(m_infoPanel, "Field", entry.getName());
		addNameValue(m_infoPanel, "Class", entry.getClassEntry().getName());
		addNameValue(m_infoPanel, "Type", entry.getType().toString());
	}
	
	private void showMethodEntry(MethodEntry entry) {
		addNameValue(m_infoPanel, "Method", entry.getName());
		addNameValue(m_infoPanel, "Class", entry.getClassEntry().getName());
		addNameValue(m_infoPanel, "Signature", entry.getSignature().toString());
	}
	
	private void showConstructorEntry(ConstructorEntry entry) {
		addNameValue(m_infoPanel, "Constructor", entry.getClassEntry().getName());
		if (!entry.isStatic()) {
			addNameValue(m_infoPanel, "Signature", entry.getSignature().toString());
		}
	}
	
	private void showArgumentEntry(ArgumentEntry entry) {
		addNameValue(m_infoPanel, "Argument", entry.getName());
		addNameValue(m_infoPanel, "Class", entry.getClassEntry().getName());
		addNameValue(m_infoPanel, "Method", entry.getBehaviorEntry().getName());
		addNameValue(m_infoPanel, "Index", Integer.toString(entry.getIndex()));
	}
	
	private void addNameValue(JPanel container, String name, String value) {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
		container.add(panel);
		
		JLabel label = new JLabel(name + ":", JLabel.RIGHT);
		label.setPreferredSize(new Dimension(100, label.getPreferredSize().height));
		panel.add(label);
		
		panel.add(GuiTricks.unboldLabel(new JLabel(value, JLabel.LEFT)));
	}
	
	private void onCaretMove(int pos) {
		
		Token token = m_controller.getToken(pos);
		boolean isToken = token != null;
		
		m_reference = m_controller.getDeobfReference(token);
		boolean isClassEntry = isToken && m_reference.entry instanceof ClassEntry;
		boolean isFieldEntry = isToken && m_reference.entry instanceof FieldEntry;
		boolean isMethodEntry = isToken && m_reference.entry instanceof MethodEntry;
		boolean isConstructorEntry = isToken && m_reference.entry instanceof ConstructorEntry;
		boolean isInJar = isToken && m_controller.entryIsInJar(m_reference.entry);
		boolean isRenameable = isToken && m_controller.referenceIsRenameable(m_reference);
		
		if (isToken) {
			showReference(m_reference);
		} else {
			clearReference();
		}
		
		m_renameMenu.setEnabled(isRenameable && isToken);
		m_showInheritanceMenu.setEnabled(isClassEntry || isMethodEntry || isConstructorEntry);
		m_showImplementationsMenu.setEnabled(isClassEntry || isMethodEntry);
		m_showCallsMenu.setEnabled(isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry);
		m_openEntryMenu.setEnabled(isInJar && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		m_openPreviousMenu.setEnabled(m_controller.hasPreviousLocation());
		m_toggleMappingMenu.setEnabled(isRenameable && isToken);
		
		if (isToken && m_controller.entryHasDeobfuscatedName(m_reference.entry)) {
			m_toggleMappingMenu.setText("Reset to obfuscated");
		} else {
			m_toggleMappingMenu.setText("Mark as deobfuscated");
		}
	}
	
	private void navigateTo(Entry entry) {
		if (!m_controller.entryIsInJar(entry)) {
			// entry is not in the jar. Ignore it
			return;
		}
		if (m_reference != null) {
			m_controller.savePreviousReference(m_reference);
		}
		m_controller.openDeclaration(entry);
	}
	
	private void navigateTo(EntryReference<Entry,Entry> reference) {
		if (!m_controller.entryIsInJar(reference.getLocationClassEntry())) {
			// reference is not in the jar. Ignore it
			return;
		}
		if (m_reference != null) {
			m_controller.savePreviousReference(m_reference);
		}
		m_controller.openReference(reference);
	}
	
	private void startRename() {
		
		// init the text box
		final JTextField text = new JTextField();
		text.setText(m_reference.getNamableName());
		text.setPreferredSize(new Dimension(360, text.getPreferredSize().height));
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						finishRename(text, true);
					break;
					
					case KeyEvent.VK_ESCAPE:
						finishRename(text, false);
					break;
				}
			}
		});
		
		// find the label with the name and replace it with the text box
		JPanel panel = (JPanel)m_infoPanel.getComponent(0);
		panel.remove(panel.getComponentCount() - 1);
		panel.add(text);
		text.grabFocus();
		text.selectAll();
		
		redraw();
	}
	
	private void finishRename(JTextField text, boolean saveName) {
		String newName = text.getText();
		if (saveName && newName != null && newName.length() > 0) {
			try {
				m_controller.rename(m_reference, newName);
			} catch (IllegalNameException ex) {
				text.setBorder(BorderFactory.createLineBorder(Color.red, 1));
				text.setToolTipText(ex.getReason());
				GuiTricks.showToolTipNow(text);
			}
			return;
		}
		
		// abort the rename
		JPanel panel = (JPanel)m_infoPanel.getComponent(0);
		panel.remove(panel.getComponentCount() - 1);
		panel.add(GuiTricks.unboldLabel(new JLabel(m_reference.getNamableName(), JLabel.LEFT)));
		
		m_editor.grabFocus();
		
		redraw();
	}
	
	private void showInheritance() {
		
		if (m_reference == null) {
			return;
		}
		
		m_inheritanceTree.setModel(null);
		
		if (m_reference.entry instanceof ClassEntry) {
			// get the class inheritance
			ClassInheritanceTreeNode classNode = m_controller.getClassInheritance((ClassEntry)m_reference.entry);
			
			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			m_inheritanceTree.setModel(new DefaultTreeModel((TreeNode)path.getPathComponent(0)));
			m_inheritanceTree.expandPath(path);
			m_inheritanceTree.setSelectionRow(m_inheritanceTree.getRowForPath(path));
		} else if (m_reference.entry instanceof MethodEntry) {
			// get the method inheritance
			MethodInheritanceTreeNode classNode = m_controller.getMethodInheritance((MethodEntry)m_reference.entry);
			
			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			m_inheritanceTree.setModel(new DefaultTreeModel((TreeNode)path.getPathComponent(0)));
			m_inheritanceTree.expandPath(path);
			m_inheritanceTree.setSelectionRow(m_inheritanceTree.getRowForPath(path));
		}
		
		m_tabs.setSelectedIndex(0);
		redraw();
	}
	
	private void showImplementations() {
		
		if (m_reference == null) {
			return;
		}
		
		m_implementationsTree.setModel(null);
		
		if (m_reference.entry instanceof ClassEntry) {
			// get the class implementations
			ClassImplementationsTreeNode node = m_controller.getClassImplementations((ClassEntry)m_reference.entry);
			if (node != null) {
				// show the tree at the root
				TreePath path = getPathToRoot(node);
				m_implementationsTree.setModel(new DefaultTreeModel((TreeNode)path.getPathComponent(0)));
				m_implementationsTree.expandPath(path);
				m_implementationsTree.setSelectionRow(m_implementationsTree.getRowForPath(path));
			}
		} else if (m_reference.entry instanceof MethodEntry) {
			// get the method implementations
			MethodImplementationsTreeNode node = m_controller.getMethodImplementations((MethodEntry)m_reference.entry);
			if (node != null) {
				// show the tree at the root
				TreePath path = getPathToRoot(node);
				m_implementationsTree.setModel(new DefaultTreeModel((TreeNode)path.getPathComponent(0)));
				m_implementationsTree.expandPath(path);
				m_implementationsTree.setSelectionRow(m_implementationsTree.getRowForPath(path));
			}
		}
		
		m_tabs.setSelectedIndex(1);
		redraw();
	}
	
	private void showCalls() {
		
		if (m_reference == null) {
			return;
		}
		
		if (m_reference.entry instanceof ClassEntry) {
			// look for calls to the default constructor
			// TODO: get a list of all the constructors and find calls to all of them
			BehaviorReferenceTreeNode node = m_controller.getMethodReferences(new ConstructorEntry((ClassEntry)m_reference.entry, new Signature("()V")));
			m_callsTree.setModel(new DefaultTreeModel(node));
		} else if (m_reference.entry instanceof FieldEntry) {
			FieldReferenceTreeNode node = m_controller.getFieldReferences((FieldEntry)m_reference.entry);
			m_callsTree.setModel(new DefaultTreeModel(node));
		} else if (m_reference.entry instanceof MethodEntry) {
			BehaviorReferenceTreeNode node = m_controller.getMethodReferences((MethodEntry)m_reference.entry);
			m_callsTree.setModel(new DefaultTreeModel(node));
		} else if (m_reference.entry instanceof ConstructorEntry) {
			BehaviorReferenceTreeNode node = m_controller.getMethodReferences((ConstructorEntry)m_reference.entry);
			m_callsTree.setModel(new DefaultTreeModel(node));
		}
		
		m_tabs.setSelectedIndex(2);
		redraw();
	}
	
	private void toggleMapping() {
		if (m_controller.entryHasDeobfuscatedName(m_reference.entry)) {
			m_controller.removeMapping(m_reference);
		} else {
			m_controller.markAsDeobfuscated(m_reference);
		}
	}
	
	private TreePath getPathToRoot(TreeNode node) {
		List<TreeNode> nodes = Lists.newArrayList();
		TreeNode n = node;
		do {
			nodes.add(n);
			n = n.getParent();
		} while (n != null);
		Collections.reverse(nodes);
		return new TreePath(nodes.toArray());
	}
	
	private void close() {
		if (!m_controller.isDirty()) {
			// everything is saved, we can exit safely
			m_frame.dispose();
		} else {
			// ask to save before closing
			String[] options = { "Save and exit", "Discard changes", "Cancel" };
			int response = JOptionPane.showOptionDialog(m_frame, "Your mappings have not been saved yet. Do you want to save?", "Save your changes?", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
			switch (response) {
				case JOptionPane.YES_OPTION: // save and exit
					if (m_mappingsFileChooser.getSelectedFile() != null || m_mappingsFileChooser.showSaveDialog(m_frame) == JFileChooser.APPROVE_OPTION) {
						try {
							m_controller.saveMappings(m_mappingsFileChooser.getSelectedFile());
							m_frame.dispose();
						} catch (IOException ex) {
							throw new Error(ex);
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
	
	private void redraw() {
		m_frame.validate();
		m_frame.repaint();
	}
}
