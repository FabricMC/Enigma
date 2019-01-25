/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui;

import com.google.common.collect.Lists;
import cuchaz.enigma.Constants;
import cuchaz.enigma.ExceptionIgnorer;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.config.Config;
import cuchaz.enigma.config.Themes;
import cuchaz.enigma.gui.dialog.CrashDialog;
import cuchaz.enigma.gui.elements.MenuBar;
import cuchaz.enigma.gui.elements.PopupMenuBar;
import cuchaz.enigma.gui.filechooser.FileChooserAny;
import cuchaz.enigma.gui.filechooser.FileChooserFolder;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.SelectionHighlightPainter;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.gui.panels.PanelDeobf;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.gui.panels.PanelIdentifier;
import cuchaz.enigma.gui.panels.PanelObf;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.Utils;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class Gui {

	public final PopupMenuBar popupMenu;
	private final PanelObf obfPanel;
	private final PanelDeobf deobfPanel;

	private final MenuBar menuBar;
	// state
	public EntryReference<Entry<?>, Entry<?>> reference;
	public FileDialog jarFileChooser;
	public FileDialog tinyMappingsFileChooser;
	public JFileChooser enigmaMappingsFileChooser;
	public JFileChooser exportSourceFileChooser;
	public FileDialog exportJarFileChooser;
	private GuiController controller;
	private JFrame frame;
	public Config.LookAndFeel editorFeel;
	public PanelEditor editor;
	private JPanel classesPanel;
	private JSplitPane splitClasses;
	private PanelIdentifier infoPanel;
	public Map<String, BoxHighlightPainter> boxHighlightPainters;
	private SelectionHighlightPainter selectionHighlightPainter;
	private JTree inheritanceTree;
	private JTree implementationsTree;
	private JTree callsTree;
	private JList<Token> tokens;
	private JTabbedPane tabs;

	public void setEditorTheme(Config.LookAndFeel feel) {
		if (editor != null && (editorFeel == null || editorFeel != feel)) {
			editor.updateUI();
			editor.setBackground(new Color(Config.getInstance().editorBackground));
			if (editorFeel != null) {
				getController().refreshCurrentClass();
			}

			editorFeel = feel;
		}
	}

	public Gui() {
		// init frame
		this.frame = new JFrame(Constants.NAME);
		final Container pane = this.frame.getContentPane();
		pane.setLayout(new BorderLayout());

		Config.getInstance().lookAndFeel.setGlobalLAF();

		if (Boolean.parseBoolean(System.getProperty("enigma.catchExceptions", "true"))) {
			// install a global exception handler to the event thread
			CrashDialog.init(this.frame);
			Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
				t.printStackTrace(System.err);
				if (!ExceptionIgnorer.shouldIgnore(t)) {
					CrashDialog.show(t);
				}
			});
		}

		this.controller = new GuiController(this);

		// init file choosers
		this.jarFileChooser = new FileDialog(getFrame(), "Open Jar", FileDialog.LOAD);

		this.tinyMappingsFileChooser = new FileDialog(getFrame(), "Open tiny Mappings", FileDialog.LOAD);
		this.enigmaMappingsFileChooser = new FileChooserAny();
		this.exportSourceFileChooser = new FileChooserFolder();
		this.exportJarFileChooser = new FileDialog(getFrame(), "Export jar", FileDialog.SAVE);

		this.obfPanel = new PanelObf(this);
		this.deobfPanel = new PanelDeobf(this);

		// set up classes panel (don't add the splitter yet)
		splitClasses = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.obfPanel, this.deobfPanel);
		splitClasses.setResizeWeight(0.3);
		this.classesPanel = new JPanel();
		this.classesPanel.setLayout(new BorderLayout());
		this.classesPanel.setPreferredSize(new Dimension(250, 0));

		// init info panel
		infoPanel = new PanelIdentifier(this);
		infoPanel.clearReference();

		// init editor
		Themes.updateTheme(this);
		selectionHighlightPainter = new SelectionHighlightPainter();
		this.editor = new PanelEditor(this);
		JScrollPane sourceScroller = new JScrollPane(this.editor);
		this.editor.setContentType("text/enigma-sources");
		this.editor.setBackground(new Color(Config.getInstance().editorBackground));
		DefaultSyntaxKit kit = (DefaultSyntaxKit) this.editor.getEditorKit();
		kit.toggleComponent(this.editor, "de.sciss.syntaxpane.components.TokenMarker");

		// init editor popup menu
		this.popupMenu = new PopupMenuBar(this);
		this.editor.setComponentPopupMenu(this.popupMenu);

		// init inheritance panel
		inheritanceTree = new JTree();
		inheritanceTree.setModel(null);
		inheritanceTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					// get the selected node
					TreePath path = inheritanceTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ClassInheritanceTreeNode) {
						ClassInheritanceTreeNode classNode = (ClassInheritanceTreeNode) node;
						navigateTo(new ClassEntry(classNode.getObfClassName()));
					} else if (node instanceof MethodInheritanceTreeNode) {
						MethodInheritanceTreeNode methodNode = (MethodInheritanceTreeNode) node;
						if (methodNode.isImplemented()) {
							navigateTo(methodNode.getMethodEntry());
						}
					}
				}
			}
		});
		JPanel inheritancePanel = new JPanel();
		inheritancePanel.setLayout(new BorderLayout());
		inheritancePanel.add(new JScrollPane(inheritanceTree));

		// init implementations panel
		implementationsTree = new JTree();
		implementationsTree.setModel(null);
		implementationsTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					// get the selected node
					TreePath path = implementationsTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ClassImplementationsTreeNode) {
						ClassImplementationsTreeNode classNode = (ClassImplementationsTreeNode) node;
						navigateTo(classNode.getClassEntry());
					} else if (node instanceof MethodImplementationsTreeNode) {
						MethodImplementationsTreeNode methodNode = (MethodImplementationsTreeNode) node;
						navigateTo(methodNode.getMethodEntry());
					}
				}
			}
		});
		JPanel implementationsPanel = new JPanel();
		implementationsPanel.setLayout(new BorderLayout());
		implementationsPanel.add(new JScrollPane(implementationsTree));

		// init call panel
		callsTree = new JTree();
		callsTree.setModel(null);
		callsTree.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					// get the selected node
					TreePath path = callsTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ReferenceTreeNode) {
						ReferenceTreeNode<Entry<?>, Entry<?>> referenceNode = ((ReferenceTreeNode<Entry<?>, Entry<?>>) node);
						if (referenceNode.getReference() != null) {
							navigateTo(referenceNode.getReference());
						} else {
							navigateTo(referenceNode.getEntry());
						}
					}
				}
			}
		});
		tokens = new JList<>();
		tokens.setCellRenderer(new TokenListCellRenderer(this.controller));
		tokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tokens.setLayoutOrientation(JList.VERTICAL);
		tokens.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					Token selected = tokens.getSelectedValue();
					if (selected != null) {
						showToken(selected);
					}
				}
			}
		});
		tokens.setPreferredSize(new Dimension(0, 200));
		tokens.setMinimumSize(new Dimension(0, 200));
		JSplitPane callPanel = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				true,
				new JScrollPane(callsTree),
				new JScrollPane(tokens)
		);
		callPanel.setResizeWeight(1); // let the top side take all the slack
		callPanel.resetToPreferredSizes();

		// layout controls
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(infoPanel, BorderLayout.NORTH);
		centerPanel.add(sourceScroller, BorderLayout.CENTER);
		tabs = new JTabbedPane();
		tabs.setPreferredSize(new Dimension(250, 0));
		tabs.addTab("Inheritance", inheritancePanel);
		tabs.addTab("Implementations", implementationsPanel);
		tabs.addTab("Call Graph", callPanel);
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, tabs);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.classesPanel, splitRight);
		splitCenter.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitCenter, BorderLayout.CENTER);

		// init menus
		this.menuBar = new MenuBar(this);
		this.frame.setJMenuBar(this.menuBar);

		// init state
		onCloseJar();

		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				close();
			}
		});

		// show the frame
		pane.doLayout();
		this.frame.setSize(1024, 576);
		this.frame.setMinimumSize(new Dimension(640, 480));
		this.frame.setVisible(true);
		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	public JFrame getFrame() {
		return this.frame;
	}

	public GuiController getController() {
		return this.controller;
	}

	public void onStartOpenJar(String message) {
		this.classesPanel.removeAll();
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		panel.add(new JLabel(message));
		this.classesPanel.add(panel);

		redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.frame.setTitle(Constants.NAME + " - " + jarName);
		this.classesPanel.removeAll();
		this.classesPanel.add(splitClasses);
		setSource(null);

		// update menu
		this.menuBar.closeJarMenu.setEnabled(true);
		this.menuBar.openTinyMappingsMenu.setEnabled(true);
		this.menuBar.openEnigmaMappingsMenu.setEnabled(true);
		this.menuBar.saveMappingsMenu.setEnabled(false);
		this.menuBar.saveMappingEnigmaFileMenu.setEnabled(true);
		this.menuBar.saveMappingEnigmaDirectoryMenu.setEnabled(true);
		this.menuBar.saveMappingsSrgMenu.setEnabled(true);
		this.menuBar.closeMappingsMenu.setEnabled(true);
		this.menuBar.exportSourceMenu.setEnabled(true);
		this.menuBar.exportJarMenu.setEnabled(true);

		redraw();
	}

	public void onCloseJar() {
		// update gui
		this.frame.setTitle(Constants.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		setSource(null);
		this.classesPanel.removeAll();

		// update menu
		this.menuBar.closeJarMenu.setEnabled(false);
		this.menuBar.openTinyMappingsMenu.setEnabled(false);
		this.menuBar.openEnigmaMappingsMenu.setEnabled(false);
		this.menuBar.saveMappingsMenu.setEnabled(false);
		this.menuBar.saveMappingEnigmaFileMenu.setEnabled(false);
		this.menuBar.saveMappingEnigmaDirectoryMenu.setEnabled(false);
		this.menuBar.saveMappingsSrgMenu.setEnabled(false);
		this.menuBar.closeMappingsMenu.setEnabled(false);
		this.menuBar.exportSourceMenu.setEnabled(false);
		this.menuBar.exportJarMenu.setEnabled(false);

		redraw();
	}

	public void setObfClasses(Collection<ClassEntry> obfClasses) {
		this.obfPanel.obfClasses.setClasses(obfClasses);
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		this.deobfPanel.deobfClasses.setClasses(deobfClasses);
	}

	public void setMappingsFile(Path path) {
		this.enigmaMappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		this.menuBar.saveMappingsMenu.setEnabled(path != null);
	}

	public void setSource(String source) {
		this.editor.getHighlighter().removeAllHighlights();
		this.editor.setText(source);
	}

	public void showToken(final Token token) {
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null!");
		}
		CodeReader.navigateToToken(this.editor, token, selectionHighlightPainter);
		redraw();
	}

	public void showTokens(Collection<Token> tokens) {
		Vector<Token> sortedTokens = new Vector<>(tokens);
		Collections.sort(sortedTokens);
		if (sortedTokens.size() > 1) {
			// sort the tokens and update the tokens panel
			this.tokens.setListData(sortedTokens);
			this.tokens.setSelectedIndex(0);
		} else {
			this.tokens.setListData(new Vector<>());
		}

		// show the first token
		showToken(sortedTokens.get(0));
	}

	public void setHighlightedTokens(Map<String, Iterable<Token>> tokens) {
		// remove any old highlighters
		this.editor.getHighlighter().removeAllHighlights();

		if (boxHighlightPainters != null) {
			for (String s : tokens.keySet()) {
				BoxHighlightPainter painter = boxHighlightPainters.get(s);
				if (painter != null) {
					setHighlightedTokens(tokens.get(s), painter);
				}
			}
		}

		redraw();
	}

	private void setHighlightedTokens(Iterable<Token> tokens, Highlighter.HighlightPainter painter) {
		for (Token token : tokens) {
			try {
				this.editor.getHighlighter().addHighlight(token.start, token.end, painter);
			} catch (BadLocationException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}

	private void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (reference == null) {
			infoPanel.clearReference();
			return;
		}

		this.reference = reference;

		infoPanel.removeAll();
		if (reference.entry instanceof ClassEntry) {
			showClassEntry((ClassEntry) this.reference.entry);
		} else if (this.reference.entry instanceof FieldEntry) {
			showFieldEntry((FieldEntry) this.reference.entry);
		} else if (this.reference.entry instanceof MethodEntry) {
			showMethodEntry((MethodEntry) this.reference.entry);
		} else if (this.reference.entry instanceof LocalVariableEntry) {
			showLocalVariableEntry((LocalVariableEntry) this.reference.entry);
		} else {
			throw new Error("Unknown entry desc: " + this.reference.entry.getClass().getName());
		}

		redraw();
	}

	private void showLocalVariableEntry(LocalVariableEntry entry) {
		addNameValue(infoPanel, "Variable", entry.getName());
		addNameValue(infoPanel, "Class", entry.getContainingClass().getFullName());
		addNameValue(infoPanel, "Method", entry.getParent().getName());
		addNameValue(infoPanel, "Index", Integer.toString(entry.getIndex()));
	}

	private void showClassEntry(ClassEntry entry) {
		addNameValue(infoPanel, "Class", entry.getFullName());
		addModifierComboBox(infoPanel, "Modifier", entry);
	}

	private void showFieldEntry(FieldEntry entry) {
		addNameValue(infoPanel, "Field", entry.getName());
		addNameValue(infoPanel, "Class", entry.getParent().getFullName());
		addNameValue(infoPanel, "TypeDescriptor", entry.getDesc().toString());
		addModifierComboBox(infoPanel, "Modifier", entry);
	}

	private void showMethodEntry(MethodEntry entry) {
		if (entry.isConstructor()) {
			addNameValue(infoPanel, "Constructor", entry.getParent().getFullName());
		} else {
			addNameValue(infoPanel, "Method", entry.getName());
			addNameValue(infoPanel, "Class", entry.getParent().getFullName());
		}
		addNameValue(infoPanel, "MethodDescriptor", entry.getDesc().toString());
		addModifierComboBox(infoPanel, "Modifier", entry);
	}

	private void addNameValue(JPanel container, String name, String value) {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));

		JLabel label = new JLabel(name + ":", JLabel.RIGHT);
		label.setPreferredSize(new Dimension(100, label.getPreferredSize().height));
		panel.add(label);

		panel.add(Utils.unboldLabel(new JLabel(value, JLabel.LEFT)));

		container.add(panel);
	}

	private JComboBox<AccessModifier> addModifierComboBox(JPanel container, String name, Entry entry) {
		if (!getController().entryIsInJar(entry))
			return null;
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
		JLabel label = new JLabel(name + ":", JLabel.RIGHT);
		label.setPreferredSize(new Dimension(100, label.getPreferredSize().height));
		panel.add(label);
		JComboBox<AccessModifier> combo = new JComboBox<>(AccessModifier.values());
		((JLabel) combo.getRenderer()).setHorizontalAlignment(JLabel.LEFT);
		combo.setPreferredSize(new Dimension(100, label.getPreferredSize().height));
		combo.setSelectedIndex(getController().getDeobfuscator().getModifier(entry).ordinal());
		combo.addItemListener(getController()::modifierChange);
		panel.add(combo);

		container.add(panel);

		return combo;
	}

	public void onCaretMove(int pos) {

		Token token = this.controller.getToken(pos);
		boolean isToken = token != null;

		reference = this.controller.getDeobfReference(token);

		Entry<?> referenceEntry = reference != null ? reference.entry : null;
		boolean isClassEntry = isToken && referenceEntry instanceof ClassEntry;
		boolean isFieldEntry = isToken && referenceEntry instanceof FieldEntry;
		boolean isMethodEntry = isToken && referenceEntry instanceof MethodEntry && !((MethodEntry) referenceEntry).isConstructor();
		boolean isConstructorEntry = isToken && referenceEntry instanceof MethodEntry && ((MethodEntry) referenceEntry).isConstructor();
		boolean isInJar = isToken && this.controller.entryIsInJar(referenceEntry);
		boolean isRenameable = isToken && this.controller.referenceIsRenameable(reference);

		if (isToken) {
			showReference(reference);
		} else {
			infoPanel.clearReference();
		}

		this.popupMenu.renameMenu.setEnabled(isRenameable);
		this.popupMenu.showInheritanceMenu.setEnabled(isClassEntry || isMethodEntry || isConstructorEntry);
		this.popupMenu.showImplementationsMenu.setEnabled(isClassEntry || isMethodEntry);
		this.popupMenu.showCallsMenu.setEnabled(isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry);
		this.popupMenu.showCallsSpecificMenu.setEnabled(isMethodEntry);
		this.popupMenu.openEntryMenu.setEnabled(isInJar && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		this.popupMenu.openPreviousMenu.setEnabled(this.controller.hasPreviousLocation());
		this.popupMenu.toggleMappingMenu.setEnabled(isRenameable);

		if (isToken && this.controller.entryHasDeobfuscatedName(referenceEntry)) {
			this.popupMenu.toggleMappingMenu.setText("Reset to obfuscated");
		} else {
			this.popupMenu.toggleMappingMenu.setText("Mark as deobfuscated");
		}
	}

	public void navigateTo(Entry<?> entry) {
		if (!this.controller.entryIsInJar(entry)) {
			// entry is not in the jar. Ignore it
			return;
		}
		if (reference != null) {
			this.controller.savePreviousReference(reference);
		}
		this.controller.openDeclaration(entry);
	}

	private void navigateTo(EntryReference<Entry<?>, Entry<?>> reference) {
		if (!this.controller.entryIsInJar(reference.getLocationClassEntry())) {
			return;
		}
		if (this.reference != null) {
			this.controller.savePreviousReference(this.reference);
		}
		this.controller.openReference(reference);
	}

	public void startRename() {

		// init the text box
		final JTextField text = new JTextField();
		text.setText(reference.getNameableName());
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
					default:
						break;
				}
			}
		});

		// find the label with the name and replace it with the text box
		JPanel panel = (JPanel) infoPanel.getComponent(0);
		panel.remove(panel.getComponentCount() - 1);
		panel.add(text);
		text.grabFocus();

		int offset = text.getText().lastIndexOf('/') + 1;
		// If it's a class and isn't in the default package, assume that it's deobfuscated.
		if (reference.getNameableEntry() instanceof ClassEntry && text.getText().contains("/") && offset != 0)
			text.select(offset, text.getText().length());
		else
			text.selectAll();

		redraw();
	}

	private void finishRename(JTextField text, boolean saveName) {
		String newName = text.getText();
		if (saveName && newName != null && !newName.isEmpty()) {
			try {
				this.controller.rename(reference, newName, true);
			} catch (IllegalNameException ex) {
				text.setBorder(BorderFactory.createLineBorder(Color.red, 1));
				text.setToolTipText(ex.getReason());
				Utils.showToolTipNow(text);
			}
			return;
		}

		// abort the rename
		JPanel panel = (JPanel) infoPanel.getComponent(0);
		panel.remove(panel.getComponentCount() - 1);
		panel.add(Utils.unboldLabel(new JLabel(reference.getNameableName(), JLabel.LEFT)));

		this.editor.grabFocus();

		redraw();
	}

	public void showInheritance() {

		if (reference == null) {
			return;
		}

		inheritanceTree.setModel(null);

		if (reference.entry instanceof ClassEntry) {
			// get the class inheritance
			ClassInheritanceTreeNode classNode = this.controller.getClassInheritance((ClassEntry) reference.entry);

			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			inheritanceTree.expandPath(path);
			inheritanceTree.setSelectionRow(inheritanceTree.getRowForPath(path));
		} else if (reference.entry instanceof MethodEntry) {
			// get the method inheritance
			MethodInheritanceTreeNode classNode = this.controller.getMethodInheritance((MethodEntry) reference.entry);

			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			inheritanceTree.expandPath(path);
			inheritanceTree.setSelectionRow(inheritanceTree.getRowForPath(path));
		}

		tabs.setSelectedIndex(0);

		redraw();
	}

	public void showImplementations() {

		if (reference == null) {
			return;
		}

		implementationsTree.setModel(null);

		DefaultMutableTreeNode node = null;

		// get the class implementations
		if (reference.entry instanceof ClassEntry)
			node = this.controller.getClassImplementations((ClassEntry) reference.entry);
		else // get the method implementations
			if (reference.entry instanceof MethodEntry)
				node = this.controller.getMethodImplementations((MethodEntry) reference.entry);

		if (node != null) {
			// show the tree at the root
			TreePath path = getPathToRoot(node);
			implementationsTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			implementationsTree.expandPath(path);
			implementationsTree.setSelectionRow(implementationsTree.getRowForPath(path));
		}

		tabs.setSelectedIndex(1);

		redraw();
	}

	public void showCalls(boolean recurse) {
		if (reference == null) {
			return;
		}

		if (reference.entry instanceof ClassEntry) {
			ClassReferenceTreeNode node = this.controller.getClassReferences((ClassEntry) reference.entry);
			callsTree.setModel(new DefaultTreeModel(node));
		} else if (reference.entry instanceof FieldEntry) {
			FieldReferenceTreeNode node = this.controller.getFieldReferences((FieldEntry) reference.entry);
			callsTree.setModel(new DefaultTreeModel(node));
		} else if (reference.entry instanceof MethodEntry) {
			MethodReferenceTreeNode node = this.controller.getMethodReferences((MethodEntry) reference.entry, recurse);
			callsTree.setModel(new DefaultTreeModel(node));
		}

		tabs.setSelectedIndex(2);

		redraw();
	}

	public void toggleMapping() {
		if (this.controller.entryHasDeobfuscatedName(reference.entry)) {
			this.controller.removeMapping(reference);
		} else {
			this.controller.markAsDeobfuscated(reference);
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

	public void showDiscardDiag(Function<Integer, Void> callback, String... options) {
		int response = JOptionPane.showOptionDialog(this.frame, "Your mappings have not been saved yet. Do you want to save?", "Save your changes?", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public void saveMapping() throws IOException {
		if (this.enigmaMappingsFileChooser.getSelectedFile() != null || this.enigmaMappingsFileChooser.showSaveDialog(this.frame) == JFileChooser.APPROVE_OPTION)
			this.controller.saveMappings(this.enigmaMappingsFileChooser.getSelectedFile().toPath());
	}

	public void close() {
		if (!this.controller.isDirty()) {
			// everything is saved, we can exit safely
			this.frame.dispose();
			System.exit(0);
		} else {
			// ask to save before closing
			showDiscardDiag((response) -> {
				if (response == JOptionPane.YES_OPTION) {
					try {
						this.saveMapping();
						this.frame.dispose();

					} catch (IOException ex) {
						throw new Error(ex);
					}
				} else if (response == JOptionPane.NO_OPTION)
					this.frame.dispose();

				return null;
			}, "Save and exit", "Discard changes", "Cancel");
		}
	}

	public void redraw() {
		this.frame.validate();
		this.frame.repaint();
	}

	public void onPanelRename(Object prevData, Object data, DefaultMutableTreeNode node) throws IllegalNameException {
		// package rename
		if (data instanceof String) {
			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
				ClassEntry prevDataChild = (ClassEntry) childNode.getUserObject();
				ClassEntry dataChild = new ClassEntry(data + "/" + prevDataChild.getSimpleName());
				this.controller.rename(new EntryReference<>(prevDataChild, prevDataChild.getFullName()), dataChild.getFullName(), false);
				childNode.setUserObject(dataChild);
			}
			node.setUserObject(data);
			// Ob package will never be modified, just reload deob view
			this.deobfPanel.deobfClasses.reload();
		}
		// class rename
		else if (data instanceof ClassEntry)
			this.controller.rename(new EntryReference<>((ClassEntry) prevData, ((ClassEntry) prevData).getFullName()), ((ClassEntry) data).getFullName(), false);
	}

	public void moveClassTree(EntryReference<Entry<?>, Entry<?>> deobfReference, String newName) {
		String oldEntry = deobfReference.entry.getContainingClass().getPackageName();
		String newEntry = new ClassEntry(newName).getPackageName();
		moveClassTree(deobfReference, newName, oldEntry == null,
				newEntry == null);
	}

	// TODO: getExpansionState will *not* actually update itself based on name changes!
	public void moveClassTree(EntryReference<Entry<?>, Entry<?>> deobfReference, String newName, boolean isOldOb, boolean isNewOb) {
		ClassEntry oldEntry = deobfReference.entry.getContainingClass();
		ClassEntry newEntry = new ClassEntry(newName);

		// Ob -> deob
		List<ClassSelector.StateEntry> stateDeobf = this.deobfPanel.deobfClasses.getExpansionState(this.deobfPanel.deobfClasses);
		List<ClassSelector.StateEntry> stateObf = this.obfPanel.obfClasses.getExpansionState(this.obfPanel.obfClasses);

		if (isOldOb && !isNewOb) {
			this.deobfPanel.deobfClasses.moveClassTree(oldEntry, newEntry, obfPanel.obfClasses);
			ClassSelectorPackageNode packageNode = this.obfPanel.obfClasses.getPackageNode(oldEntry);
			this.obfPanel.obfClasses.removeNode(packageNode, oldEntry);
			this.obfPanel.obfClasses.removeNodeIfEmpty(packageNode);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Deob -> ob
		else if (isNewOb && !isOldOb) {
			this.obfPanel.obfClasses.moveClassTree(oldEntry, newEntry, deobfPanel.deobfClasses);
			ClassSelectorPackageNode packageNode = this.deobfPanel.deobfClasses.getPackageNode(oldEntry);
			this.deobfPanel.deobfClasses.removeNode(packageNode, oldEntry);
			this.deobfPanel.deobfClasses.removeNodeIfEmpty(packageNode);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Local move
		else if (isOldOb) {
			this.obfPanel.obfClasses.moveClassTree(oldEntry, newEntry, null);
			this.obfPanel.obfClasses.removeNodeIfEmpty(this.obfPanel.obfClasses.getPackageNode(oldEntry));
			this.obfPanel.obfClasses.reload();
		} else {
			this.deobfPanel.deobfClasses.moveClassTree(oldEntry, newEntry, null);
			this.deobfPanel.deobfClasses.removeNodeIfEmpty(this.deobfPanel.deobfClasses.getPackageNode(oldEntry));
			this.deobfPanel.deobfClasses.reload();
		}

		this.deobfPanel.deobfClasses.restoreExpansionState(this.deobfPanel.deobfClasses, stateDeobf);
		this.obfPanel.obfClasses.restoreExpansionState(this.obfPanel.obfClasses, stateObf);
	}

	public PanelDeobf getDeobfPanel() {
		return deobfPanel;
	}
}
