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

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Function;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.tree.*;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import cuchaz.enigma.Constants;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.ExceptionIgnorer;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.config.Config;
import cuchaz.enigma.config.Themes;
import cuchaz.enigma.gui.dialog.CrashDialog;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.elements.MenuBar;
import cuchaz.enigma.gui.elements.PopupMenuBar;
import cuchaz.enigma.gui.filechooser.FileChooserAny;
import cuchaz.enigma.gui.filechooser.FileChooserFolder;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.SelectionHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import cuchaz.enigma.gui.panels.PanelDeobf;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.gui.panels.PanelIdentifier;
import cuchaz.enigma.gui.panels.PanelObf;
import cuchaz.enigma.gui.util.History;
import cuchaz.enigma.network.packet.ChangeDocsC2SPacket;
import cuchaz.enigma.network.packet.MarkDeobfuscatedC2SPacket;
import cuchaz.enigma.network.packet.RemoveMappingC2SPacket;
import cuchaz.enigma.network.packet.RenameC2SPacket;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Message;
import cuchaz.enigma.utils.Utils;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class Gui {

	public final PopupMenuBar popupMenu;
	private final PanelObf obfPanel;
	private final PanelDeobf deobfPanel;

	private final MenuBar menuBar;
	// state
	public History<EntryReference<Entry<?>, Entry<?>>> referenceHistory;
	public EntryReference<Entry<?>, Entry<?>> cursorReference;
	private boolean shouldNavigateOnClick;

	public FileDialog jarFileChooser;
	public FileDialog tinyMappingsFileChooser;
	public JFileChooser enigmaMappingsFileChooser;
	public JFileChooser exportSourceFileChooser;
	public FileDialog exportJarFileChooser;
	private GuiController controller;
	private JFrame frame;
	public Config.LookAndFeel editorFeel;
	public PanelEditor editor;
	public JScrollPane sourceScroller;
	private JPanel classesPanel;
	private JSplitPane splitClasses;
	private PanelIdentifier infoPanel;
	public Map<TokenHighlightType, BoxHighlightPainter> boxHighlightPainters;
	private SelectionHighlightPainter selectionHighlightPainter;
	private JTree inheritanceTree;
	private JTree implementationsTree;
	private JTree callsTree;
	private JList<Token> tokens;
	private JTabbedPane tabs;

	private JSplitPane logSplit;
	private JTabbedPane logTabs;
	private JList<String> users;
	private DefaultListModel<String> userModel;
	private JList<Message> messages;
	private DefaultListModel<Message> messageModel;

	private JPanel statusBar;
	private JLabel connectionStatusLabel;
	private JLabel lastActionLabel;

	public JTextField renameTextField;
	public JTextArea javadocTextArea;

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

	public Gui(EnigmaProfile profile) {
		Config.getInstance().lookAndFeel.setGlobalLAF();

		// init frame
		this.frame = new JFrame(Constants.NAME);
		final Container pane = this.frame.getContentPane();
		pane.setLayout(new BorderLayout());

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

		this.controller = new GuiController(this, profile);

		// init file choosers
		this.jarFileChooser = new FileDialog(getFrame(), I18n.translate("menu.file.jar.open"), FileDialog.LOAD);

		this.tinyMappingsFileChooser = new FileDialog(getFrame(), "Open tiny Mappings", FileDialog.LOAD);
		this.enigmaMappingsFileChooser = new FileChooserAny();
		this.exportSourceFileChooser = new FileChooserFolder();
		this.exportJarFileChooser = new FileDialog(getFrame(), I18n.translate("menu.file.export.jar"), FileDialog.SAVE);

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
		this.sourceScroller = new JScrollPane(this.editor);
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
				if (event.getClickCount() >= 2) {
					// get the selected node
					TreePath path = inheritanceTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ClassInheritanceTreeNode) {
						ClassInheritanceTreeNode classNode = (ClassInheritanceTreeNode) node;
						controller.navigateTo(new ClassEntry(classNode.getObfClassName()));
					} else if (node instanceof MethodInheritanceTreeNode) {
						MethodInheritanceTreeNode methodNode = (MethodInheritanceTreeNode) node;
						if (methodNode.isImplemented()) {
							controller.navigateTo(methodNode.getMethodEntry());
						}
					}
				}
			}
		});
		TreeCellRenderer cellRenderer = inheritanceTree.getCellRenderer();
		inheritanceTree.setCellRenderer(new MethodTreeCellRenderer(cellRenderer));

		JPanel inheritancePanel = new JPanel();
		inheritancePanel.setLayout(new BorderLayout());
		inheritancePanel.add(new JScrollPane(inheritanceTree));

		// init implementations panel
		implementationsTree = new JTree();
		implementationsTree.setModel(null);
		implementationsTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() >= 2) {
					// get the selected node
					TreePath path = implementationsTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ClassImplementationsTreeNode) {
						ClassImplementationsTreeNode classNode = (ClassImplementationsTreeNode) node;
						controller.navigateTo(classNode.getClassEntry());
					} else if (node instanceof MethodImplementationsTreeNode) {
						MethodImplementationsTreeNode methodNode = (MethodImplementationsTreeNode) node;
						controller.navigateTo(methodNode.getMethodEntry());
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
				if (event.getClickCount() >= 2) {
					// get the selected node
					TreePath path = callsTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ReferenceTreeNode) {
						ReferenceTreeNode<Entry<?>, Entry<?>> referenceNode = ((ReferenceTreeNode<Entry<?>, Entry<?>>) node);
						if (referenceNode.getReference() != null) {
							controller.navigateTo(referenceNode.getReference());
						} else {
							controller.navigateTo(referenceNode.getEntry());
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
		tabs.addTab(I18n.translate("info_panel.tree.inheritance"), inheritancePanel);
		tabs.addTab(I18n.translate("info_panel.tree.implementations"), implementationsPanel);
		tabs.addTab(I18n.translate("info_panel.tree.calls"), callPanel);
		logTabs = new JTabbedPane(JTabbedPane.BOTTOM);
		userModel = new DefaultListModel<>();
		users = new JList<>(userModel);
		messageModel = new DefaultListModel<>();
		messages = new JList<>(messageModel);
		messages.setCellRenderer(new MessageListCellRenderer());
		logTabs.addTab(I18n.translate("log_panel.users"), this.users);
		logTabs.addTab(I18n.translate("log_panel.messages"), this.messages);
		logSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabs, logTabs);
		logSplit.setResizeWeight(0.5);
		logSplit.resetToPreferredSizes();
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, this.logSplit);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.classesPanel, splitRight);
		splitCenter.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitCenter, BorderLayout.CENTER);

		// init menus
		this.menuBar = new MenuBar(this);
		this.frame.setJMenuBar(this.menuBar);

		// init status bar
		statusBar = new JPanel(new BorderLayout());
		statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
		connectionStatusLabel = new JLabel("Disconnected.");
		lastActionLabel = new JLabel("Ready.");
		statusBar.add(lastActionLabel, BorderLayout.CENTER);
		statusBar.add(connectionStatusLabel, BorderLayout.EAST);
		pane.add(statusBar, BorderLayout.SOUTH);

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

	public void onStartOpenJar() {
		this.classesPanel.removeAll();
		redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.frame.setTitle(Constants.NAME + " - " + jarName);
		this.classesPanel.removeAll();
		this.classesPanel.add(splitClasses);
		setEditorText(null);

		// update menu
		this.menuBar.closeJarMenu.setEnabled(true);
		this.menuBar.openMappingsMenus.forEach(item -> item.setEnabled(true));
		this.menuBar.saveMappingsMenu.setEnabled(false);
		this.menuBar.saveMappingsMenus.forEach(item -> item.setEnabled(true));
		this.menuBar.closeMappingsMenu.setEnabled(true);
		this.menuBar.exportSourceMenu.setEnabled(true);
		this.menuBar.exportJarMenu.setEnabled(true);
		this.menuBar.connectToServerMenu.setEnabled(true);
		this.menuBar.startServerMenu.setEnabled(true);

		redraw();
	}

	public void onCloseJar() {
		// update gui
		this.frame.setTitle(Constants.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		setEditorText(null);
		this.classesPanel.removeAll();

		// update menu
		this.menuBar.closeJarMenu.setEnabled(false);
		this.menuBar.openMappingsMenus.forEach(item -> item.setEnabled(false));
		this.menuBar.saveMappingsMenu.setEnabled(false);
		this.menuBar.saveMappingsMenus.forEach(item -> item.setEnabled(false));
		this.menuBar.closeMappingsMenu.setEnabled(false);
		this.menuBar.exportSourceMenu.setEnabled(false);
		this.menuBar.exportJarMenu.setEnabled(false);
		this.menuBar.connectToServerMenu.setEnabled(false);
		this.menuBar.startServerMenu.setEnabled(false);

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

	public void setEditorText(String source) {
		this.editor.getHighlighter().removeAllHighlights();
		this.editor.setText(source);
	}

	public void setSource(DecompiledClassSource source) {
		editor.setText(source.toString());
		setHighlightedTokens(source.getHighlightedTokens());
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

	public void setHighlightedTokens(Map<TokenHighlightType, Collection<Token>> tokens) {
		// remove any old highlighters
		this.editor.getHighlighter().removeAllHighlights();

		if (boxHighlightPainters != null) {
			for (TokenHighlightType type : tokens.keySet()) {
				BoxHighlightPainter painter = boxHighlightPainters.get(type);
				if (painter != null) {
					setHighlightedTokens(tokens.get(type), painter);
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

	private void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (reference == null) {
			infoPanel.clearReference();
			return;
		}

		this.cursorReference = reference;

		EntryReference<Entry<?>, Entry<?>> translatedReference = controller.project.getMapper().deobfuscate(reference);

		infoPanel.removeAll();
		if (translatedReference.entry instanceof ClassEntry) {
			showClassEntry((ClassEntry) translatedReference.entry);
		} else if (translatedReference.entry instanceof FieldEntry) {
			showFieldEntry((FieldEntry) translatedReference.entry);
		} else if (translatedReference.entry instanceof MethodEntry) {
			showMethodEntry((MethodEntry) translatedReference.entry);
		} else if (translatedReference.entry instanceof LocalVariableEntry) {
			showLocalVariableEntry((LocalVariableEntry) translatedReference.entry);
		} else {
			throw new Error("Unknown entry desc: " + translatedReference.entry.getClass().getName());
		}

		redraw();
	}

	private void showLocalVariableEntry(LocalVariableEntry entry) {
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.variable"), entry.getName());
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.class"), entry.getContainingClass().getFullName());
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.method"), entry.getParent().getName());
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.index"), Integer.toString(entry.getIndex()));
	}

	private void showClassEntry(ClassEntry entry) {
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.class"), entry.getFullName());
		addModifierComboBox(infoPanel, I18n.translate("info_panel.identifier.modifier"), entry);
	}

	private void showFieldEntry(FieldEntry entry) {
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.field"), entry.getName());
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.class"), entry.getParent().getFullName());
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.type_descriptor"), entry.getDesc().toString());
		addModifierComboBox(infoPanel, I18n.translate("info_panel.identifier.modifier"), entry);
	}

	private void showMethodEntry(MethodEntry entry) {
		if (entry.isConstructor()) {
			addNameValue(infoPanel, I18n.translate("info_panel.identifier.constructor"), entry.getParent().getFullName());
		} else {
			addNameValue(infoPanel, I18n.translate("info_panel.identifier.method"), entry.getName());
			addNameValue(infoPanel, I18n.translate("info_panel.identifier.class"), entry.getParent().getFullName());
		}
		addNameValue(infoPanel, I18n.translate("info_panel.identifier.method_descriptor"), entry.getDesc().toString());
		addModifierComboBox(infoPanel, I18n.translate("info_panel.identifier.modifier"), entry);
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

	private JComboBox<AccessModifier> addModifierComboBox(JPanel container, String name, Entry<?> entry) {
		if (!getController().project.isRenamable(entry))
			return null;
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
		JLabel label = new JLabel(name + ":", JLabel.RIGHT);
		label.setPreferredSize(new Dimension(100, label.getPreferredSize().height));
		panel.add(label);
		JComboBox<AccessModifier> combo = new JComboBox<>(AccessModifier.values());
		((JLabel) combo.getRenderer()).setHorizontalAlignment(JLabel.LEFT);
		combo.setPreferredSize(new Dimension(100, label.getPreferredSize().height));

		EntryMapping mapping = controller.project.getMapper().getDeobfMapping(entry);
		if (mapping != null) {
			combo.setSelectedIndex(mapping.getAccessModifier().ordinal());
		} else {
			combo.setSelectedIndex(AccessModifier.UNCHANGED.ordinal());
		}

		combo.addItemListener(controller::modifierChange);

		panel.add(combo);

		container.add(panel);

		return combo;
	}

	public void onCaretMove(int pos, boolean fromClick) {
		if (controller.project == null)
			return;
		EntryRemapper mapper = controller.project.getMapper();
		Token token = this.controller.getToken(pos);
		boolean isToken = token != null;

		cursorReference = this.controller.getReference(token);
		Entry<?> referenceEntry = cursorReference != null ? cursorReference.entry : null;

		if (referenceEntry != null && shouldNavigateOnClick && fromClick) {
			shouldNavigateOnClick = false;
			Entry<?> navigationEntry = referenceEntry;
			if (cursorReference.context == null) {
				EntryResolver resolver = mapper.getObfResolver();
				navigationEntry = resolver.resolveFirstEntry(referenceEntry, ResolutionStrategy.RESOLVE_ROOT);
			}
			controller.navigateTo(navigationEntry);
			return;
		}

		boolean isClassEntry = isToken && referenceEntry instanceof ClassEntry;
		boolean isFieldEntry = isToken && referenceEntry instanceof FieldEntry;
		boolean isMethodEntry = isToken && referenceEntry instanceof MethodEntry && !((MethodEntry) referenceEntry).isConstructor();
		boolean isConstructorEntry = isToken && referenceEntry instanceof MethodEntry && ((MethodEntry) referenceEntry).isConstructor();
		boolean isRenamable = isToken && this.controller.project.isRenamable(cursorReference);

		if (isToken) {
			showCursorReference(cursorReference);
		} else {
			infoPanel.clearReference();
		}

		this.popupMenu.renameMenu.setEnabled(isRenamable);
		this.popupMenu.editJavadocMenu.setEnabled(isRenamable);
		this.popupMenu.showInheritanceMenu.setEnabled(isClassEntry || isMethodEntry || isConstructorEntry);
		this.popupMenu.showImplementationsMenu.setEnabled(isClassEntry || isMethodEntry);
		this.popupMenu.showCallsMenu.setEnabled(isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry);
		this.popupMenu.showCallsSpecificMenu.setEnabled(isMethodEntry);
		this.popupMenu.openEntryMenu.setEnabled(isRenamable && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		this.popupMenu.openPreviousMenu.setEnabled(this.controller.hasPreviousReference());
		this.popupMenu.openNextMenu.setEnabled(this.controller.hasNextReference());
		this.popupMenu.toggleMappingMenu.setEnabled(isRenamable);

		if (isToken && !Objects.equals(referenceEntry, mapper.deobfuscate(referenceEntry))) {
			this.popupMenu.toggleMappingMenu.setText(I18n.translate("popup_menu.reset_obfuscated"));
		} else {
			this.popupMenu.toggleMappingMenu.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		}
	}

	public void startDocChange() {

		// init the text box
		javadocTextArea = new JTextArea(10, 40);

		EntryReference<Entry<?>, Entry<?>> translatedReference = controller.project.getMapper().deobfuscate(cursorReference);
		javadocTextArea.setText(Strings.nullToEmpty(translatedReference.entry.getJavadocs()));

		JavadocDialog.init(frame, javadocTextArea, this::finishDocChange);
		javadocTextArea.grabFocus();

		redraw();
	}

	private void finishDocChange(JFrame ui, boolean saveName) {
		String newName = javadocTextArea.getText();
		if (saveName) {
			try {
				this.controller.changeDocs(cursorReference, newName);
				this.controller.sendPacket(new ChangeDocsC2SPacket(cursorReference.getNameableEntry(), newName));
			} catch (IllegalNameException ex) {
				javadocTextArea.setBorder(BorderFactory.createLineBorder(Color.red, 1));
				javadocTextArea.setToolTipText(ex.getReason());
				Utils.showToolTipNow(javadocTextArea);
				return;
			}

			ui.setVisible(false);
			showCursorReference(cursorReference);
			return;
		}

		// abort the jd change
		javadocTextArea = null;
		ui.setVisible(false);
		showCursorReference(cursorReference);

		this.editor.grabFocus();

		redraw();
	}

	public void startRename() {

		// init the text box
		renameTextField = new JTextField();

		EntryReference<Entry<?>, Entry<?>> translatedReference = controller.project.getMapper().deobfuscate(cursorReference);
		renameTextField.setText(translatedReference.getNameableName());

		renameTextField.setPreferredSize(new Dimension(360, renameTextField.getPreferredSize().height));
		renameTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						finishRename(true);
						break;

					case KeyEvent.VK_ESCAPE:
						finishRename(false);
						break;
					default:
						break;
				}
			}
		});

		// find the label with the name and replace it with the text box
		JPanel panel = (JPanel) infoPanel.getComponent(0);
		panel.remove(panel.getComponentCount() - 1);
		panel.add(renameTextField);
		renameTextField.grabFocus();

		int offset = renameTextField.getText().lastIndexOf('/') + 1;
		// If it's a class and isn't in the default package, assume that it's deobfuscated.
		if (translatedReference.getNameableEntry() instanceof ClassEntry && renameTextField.getText().contains("/") && offset != 0)
			renameTextField.select(offset, renameTextField.getText().length());
		else
			renameTextField.selectAll();

		redraw();
	}

	private void finishRename(boolean saveName) {
		String newName = renameTextField.getText();
		if (saveName && newName != null && !newName.isEmpty()) {
			try {
				this.controller.rename(cursorReference, newName, true);
				this.controller.sendPacket(new RenameC2SPacket(cursorReference.getNameableEntry(), newName, true));
			} catch (IllegalNameException ex) {
				renameTextField.setBorder(BorderFactory.createLineBorder(Color.red, 1));
				renameTextField.setToolTipText(ex.getReason());
				Utils.showToolTipNow(renameTextField);
			}
			return;
		}

		// abort the rename
		JPanel panel = (JPanel) infoPanel.getComponent(0);
		panel.remove(panel.getComponentCount() - 1);
		panel.add(Utils.unboldLabel(new JLabel(cursorReference.getNameableName(), JLabel.LEFT)));

		renameTextField = null;

		this.editor.grabFocus();

		redraw();
	}

	public void showInheritance() {

		if (cursorReference == null) {
			return;
		}

		inheritanceTree.setModel(null);

		if (cursorReference.entry instanceof ClassEntry) {
			// get the class inheritance
			ClassInheritanceTreeNode classNode = this.controller.getClassInheritance((ClassEntry) cursorReference.entry);

			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			inheritanceTree.expandPath(path);
			inheritanceTree.setSelectionRow(inheritanceTree.getRowForPath(path));
		} else if (cursorReference.entry instanceof MethodEntry) {
			// get the method inheritance
			MethodInheritanceTreeNode classNode = this.controller.getMethodInheritance((MethodEntry) cursorReference.entry);

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

		if (cursorReference == null) {
			return;
		}

		implementationsTree.setModel(null);

		DefaultMutableTreeNode node = null;

		// get the class implementations
		if (cursorReference.entry instanceof ClassEntry)
			node = this.controller.getClassImplementations((ClassEntry) cursorReference.entry);
		else // get the method implementations
			if (cursorReference.entry instanceof MethodEntry)
				node = this.controller.getMethodImplementations((MethodEntry) cursorReference.entry);

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
		if (cursorReference == null) {
			return;
		}

		if (cursorReference.entry instanceof ClassEntry) {
			ClassReferenceTreeNode node = this.controller.getClassReferences((ClassEntry) cursorReference.entry);
			callsTree.setModel(new DefaultTreeModel(node));
		} else if (cursorReference.entry instanceof FieldEntry) {
			FieldReferenceTreeNode node = this.controller.getFieldReferences((FieldEntry) cursorReference.entry);
			callsTree.setModel(new DefaultTreeModel(node));
		} else if (cursorReference.entry instanceof MethodEntry) {
			MethodReferenceTreeNode node = this.controller.getMethodReferences((MethodEntry) cursorReference.entry, recurse);
			callsTree.setModel(new DefaultTreeModel(node));
		}

		tabs.setSelectedIndex(2);

		redraw();
	}

	public void toggleMapping() {
		Entry<?> obfEntry = cursorReference.entry;
		Entry<?> deobfEntry = controller.project.getMapper().deobfuscate(obfEntry);

		if (!Objects.equals(obfEntry, deobfEntry)) {
			this.controller.removeMapping(cursorReference);
			this.controller.sendPacket(new RemoveMappingC2SPacket(cursorReference.getNameableEntry()));
		} else {
			this.controller.markAsDeobfuscated(cursorReference);
			this.controller.sendPacket(new MarkDeobfuscatedC2SPacket(cursorReference.getNameableEntry()));
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
		int response = JOptionPane.showOptionDialog(this.frame, I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public void saveMapping() {
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
					this.saveMapping();
					this.frame.dispose();
				} else if (response == JOptionPane.NO_OPTION) {
					this.frame.dispose();
				}

				return null;
			}, I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.close.cancel"));
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
				this.controller.sendPacket(new RenameC2SPacket(prevDataChild, dataChild.getFullName(), false));
				childNode.setUserObject(dataChild);
			}
			node.setUserObject(data);
			// Ob package will never be modified, just reload deob view
			this.deobfPanel.deobfClasses.reload();
		}
		// class rename
		else if (data instanceof ClassEntry) {
			this.controller.rename(new EntryReference<>((ClassEntry) prevData, ((ClassEntry) prevData).getFullName()), ((ClassEntry) data).getFullName(), false);
			this.controller.sendPacket(new RenameC2SPacket((ClassEntry) prevData, ((ClassEntry) data).getFullName(), false));
		}
	}

	public void moveClassTree(EntryReference<Entry<?>, Entry<?>> obfReference, String newName) {
		String oldEntry = obfReference.entry.getContainingClass().getPackageName();
		String newEntry = new ClassEntry(newName).getPackageName();
		moveClassTree(obfReference, oldEntry == null, newEntry == null);
	}

	// TODO: getExpansionState will *not* actually update itself based on name changes!
	public void moveClassTree(EntryReference<Entry<?>, Entry<?>> obfReference, boolean isOldOb, boolean isNewOb) {
		ClassEntry classEntry = obfReference.entry.getContainingClass();

		// Ob -> deob
		List<ClassSelector.StateEntry> stateDeobf = this.deobfPanel.deobfClasses.getExpansionState(this.deobfPanel.deobfClasses);
		List<ClassSelector.StateEntry> stateObf = this.obfPanel.obfClasses.getExpansionState(this.obfPanel.obfClasses);

		if (isOldOb && !isNewOb) {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.moveClassOut(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Deob -> ob
		else if (isNewOb && !isOldOb) {
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.moveClassOut(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Local move
		else if (isOldOb) {
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.reload();
		} else {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.reload();
		}

		this.deobfPanel.deobfClasses.restoreExpansionState(this.deobfPanel.deobfClasses, stateDeobf);
		this.obfPanel.obfClasses.restoreExpansionState(this.obfPanel.obfClasses, stateObf);
	}

	public PanelDeobf getDeobfPanel() {
		return deobfPanel;
	}

	public void setShouldNavigateOnClick(boolean shouldNavigateOnClick) {
		this.shouldNavigateOnClick = shouldNavigateOnClick;
	}

	public MenuBar getMenuBar() {
		return menuBar;
	}

	public void addMessage(Message message) {
		messageModel.addElement(message);
	}

	public void setUserList(List<String> users) {
		userModel.clear();
		users.forEach(userModel::addElement);
	}

}
