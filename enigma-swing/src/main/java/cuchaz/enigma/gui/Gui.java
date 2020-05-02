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

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.*;

import com.google.common.collect.Lists;
import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.gui.config.Config;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.dialog.CrashDialog;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.elements.CollapsibleTabbedPane;
import cuchaz.enigma.gui.elements.MenuBar;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.filechooser.FileChooserAny;
import cuchaz.enigma.gui.filechooser.FileChooserFolder;
import cuchaz.enigma.gui.highlight.SelectionHighlightPainter;
import cuchaz.enigma.gui.panels.PanelDeobf;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.gui.panels.PanelIdentifier;
import cuchaz.enigma.gui.panels.PanelObf;
import cuchaz.enigma.gui.util.ClassHandle;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.History;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.packet.MarkDeobfuscatedC2SPacket;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.network.packet.RemoveMappingC2SPacket;
import cuchaz.enigma.network.packet.RenameC2SPacket;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.IllegalNameException;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.network.Message;
import cuchaz.enigma.utils.I18n;

public class Gui {

	private final PanelObf obfPanel;
	private final PanelDeobf deobfPanel;

	private final MenuBar menuBar;

	// state
	public History<EntryReference<Entry<?>, Entry<?>>> referenceHistory;
	public EntryReference<Entry<?>, Entry<?>> renamingReference;
	private ConnectionState connectionState;
	private boolean isJarOpen;

	public FileDialog jarFileChooser;
	public FileDialog tinyMappingsFileChooser;
	public SearchDialog searchDialog;
	public JFileChooser enigmaMappingsFileChooser;
	public JFileChooser exportSourceFileChooser;
	public FileDialog exportJarFileChooser;
	private GuiController controller;
	private JFrame frame;
	private JPanel classesPanel;
	private JSplitPane splitClasses;
	private PanelIdentifier infoPanel;
	private JTree inheritanceTree;
	private JTree implementationsTree;
	private JTree callsTree;
	private JList<Token> tokens;
	private JTabbedPane tabs;

	private JSplitPane splitRight;
	private JSplitPane logSplit;
	private CollapsibleTabbedPane logTabs;
	private JList<String> users;
	private DefaultListModel<String> userModel;
	private JScrollPane messageScrollPane;
	private JList<Message> messages;
	private DefaultListModel<Message> messageModel;
	private JTextField chatBox;

	private JPanel statusBar;
	private JLabel connectionStatusLabel;
	private JLabel statusLabel;

	private final JTabbedPane openFiles;
	private final HashMap<ClassEntry, PanelEditor> editors = new HashMap<>();
	private final HashMap<ClassEntry, JScrollPane> components = new HashMap<>();

	public JTextField renameTextField;

	public Gui(EnigmaProfile profile) {
		Config.getInstance().lookAndFeel.setGlobalLAF();

		// init frame
		this.frame = new JFrame(Enigma.NAME);
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

		Themes.addListener((lookAndFeel, boxHighlightPainters) -> SwingUtilities.updateComponentTreeUI(getFrame()));

		Themes.updateTheme();

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
		this.classesPanel.setPreferredSize(ScaleUtil.getDimension(250, 0));

		// init info panel
		infoPanel = new PanelIdentifier(this);
		infoPanel.clearReference();

		// init editor

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
		tokens.setCellRenderer(new TokenListCellRenderer(controller));
		tokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tokens.setLayoutOrientation(JList.VERTICAL);
		tokens.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					Token selected = tokens.getSelectedValue();
					if (selected != null) {
						showToken(openClass(controller.getTokenHandle().getRef()), selected);
					}
				}
			}
		});
		tokens.setPreferredSize(ScaleUtil.getDimension(0, 200));
		tokens.setMinimumSize(ScaleUtil.getDimension(0, 200));
		JSplitPane callPanel = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				true,
				new JScrollPane(callsTree),
				new JScrollPane(tokens)
		);
		callPanel.setResizeWeight(1); // let the top side take all the slack
		callPanel.resetToPreferredSizes();

		openFiles = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

		// layout controls
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(infoPanel, BorderLayout.NORTH);
		centerPanel.add(openFiles, BorderLayout.CENTER);
		tabs = new JTabbedPane();
		tabs.setPreferredSize(ScaleUtil.getDimension(250, 0));
		tabs.addTab(I18n.translate("info_panel.tree.inheritance"), inheritancePanel);
		tabs.addTab(I18n.translate("info_panel.tree.implementations"), implementationsPanel);
		tabs.addTab(I18n.translate("info_panel.tree.calls"), callPanel);
		logTabs = new CollapsibleTabbedPane(JTabbedPane.BOTTOM);
		userModel = new DefaultListModel<>();
		users = new JList<>(userModel);
		messageModel = new DefaultListModel<>();
		messages = new JList<>(messageModel);
		messages.setCellRenderer(new MessageListCellRenderer());
		JPanel messagePanel = new JPanel(new BorderLayout());
		messageScrollPane = new JScrollPane(this.messages);
		messagePanel.add(messageScrollPane, BorderLayout.CENTER);
		JPanel chatPanel = new JPanel(new BorderLayout());
		chatBox = new JTextField();
		AbstractAction sendListener = new AbstractAction("Send") {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		};
		chatBox.addActionListener(sendListener);
		JButton chatSendButton = new JButton(sendListener);
		chatPanel.add(chatBox, BorderLayout.CENTER);
		chatPanel.add(chatSendButton, BorderLayout.EAST);
		messagePanel.add(chatPanel, BorderLayout.SOUTH);
		logTabs.addTab(I18n.translate("log_panel.users"), new JScrollPane(this.users));
		logTabs.addTab(I18n.translate("log_panel.messages"), messagePanel);
		logSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabs, logTabs);
		logSplit.setResizeWeight(0.5);
		logSplit.resetToPreferredSizes();
		splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, this.logSplit);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.classesPanel, splitRight);
		splitCenter.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitCenter, BorderLayout.CENTER);

		// init menus
		this.menuBar = new MenuBar(this);
		this.frame.setJMenuBar(this.menuBar.getUi());

		// init status bar
		statusBar = new JPanel(new BorderLayout());
		statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
		connectionStatusLabel = new JLabel();
		statusLabel = new JLabel();
		statusBar.add(statusLabel, BorderLayout.CENTER);
		statusBar.add(connectionStatusLabel, BorderLayout.EAST);
		pane.add(statusBar, BorderLayout.SOUTH);

		// init state
		setConnectionState(ConnectionState.NOT_CONNECTED);
		onCloseJar();

		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				close();
			}
		});

		// show the frame
		pane.doLayout();
		this.frame.setSize(ScaleUtil.getDimension(1024, 576));
		this.frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		this.frame.setVisible(true);
		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.frame.setLocationRelativeTo(null);
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
		this.frame.setTitle(Enigma.NAME + " - " + jarName);
		this.classesPanel.removeAll();
		this.classesPanel.add(splitClasses);
		closeAllEditorTabs();

		// update menu
		isJarOpen = true;

		updateUiState();
		redraw();
	}

	public void onCloseJar() {

		// update gui
		this.frame.setTitle(Enigma.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		closeAllEditorTabs();
		this.classesPanel.removeAll();

		// update menu
		isJarOpen = false;
		setMappingsFile(null);

		updateUiState();
		redraw();
	}

	public PanelEditor openClass(ClassEntry entry) {
		PanelEditor panelEditor = editors.computeIfAbsent(entry, e -> {
			ClassHandle ch = controller.getClassHandleProvider().openClass(entry);
			if (ch == null) return null;
			PanelEditor ed = new PanelEditor(this, ch);
			JScrollPane scrollPane = new JScrollPane(ed.getUi());
			openFiles.addTab(ed.getFileName(), scrollPane);
			components.put(e, scrollPane);
			ed.addListener(new EditorActionListener() {
				@Override
				public void onCursorReferenceChanged(PanelEditor editor, EntryReference<Entry<?>, Entry<?>> ref) {
					updateSelectedReference(editor, ref);
				}

				@Override
				public void onClassHandleChanged(PanelEditor editor, ClassEntry old, ClassHandle ch) {
					editors.remove(old);
					JScrollPane scrollPane = Objects.requireNonNull(components.remove(old));
					editors.put(ch.getRef(), editor);
					components.put(ch.getRef(), scrollPane);
				}

				@Override
				public void onTitleChanged(PanelEditor editor, String title) {
					openFiles.setTitleAt(openFiles.indexOfComponent(editor.getUi()), editor.getFileName());
				}
			});
			return ed;
		});
		if (panelEditor != null) {
			openFiles.setSelectedComponent(components.get(entry));
		}
		return panelEditor;
	}

	public void setObfClasses(Collection<ClassEntry> obfClasses) {
		this.obfPanel.obfClasses.setClasses(obfClasses);
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		this.deobfPanel.deobfClasses.setClasses(deobfClasses);
	}

	public void setMappingsFile(Path path) {
		this.enigmaMappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		updateUiState();
	}

	public void closeAllEditorTabs() {
		for (Iterator<PanelEditor> iter = editors.values().iterator(); iter.hasNext(); ) {
			PanelEditor e = iter.next();
			openFiles.remove(e.getUi());
			components.remove(e.getClassHandle().getRef());
			e.destroy();
			iter.remove();
		}
	}

	public void showToken(PanelEditor editor, Token token) {
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null!");
		}
		CodeReader.navigateToToken(editor.getUi(), token, SelectionHighlightPainter.INSTANCE);
		redraw();
	}

	public void showTokens(PanelEditor editor, Collection<Token> tokens) {
		Vector<Token> sortedTokens = new Vector<>(tokens);
		Collections.sort(sortedTokens);
		if (sortedTokens.size() > 1) {
			// sort the tokens and update the tokens panel
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			this.tokens.setListData(sortedTokens);
			this.tokens.setSelectedIndex(0);
		} else {
			this.tokens.setListData(new Vector<>());
		}

		// show the first token
		showToken(editor, sortedTokens.get(0));
	}

	private void updateSelectedReference(PanelEditor editor, EntryReference<Entry<?>, Entry<?>> ref) {
		if (editor != getActiveEditor()) return;

		if (ref != null) {
			showCursorReference(ref);
		} else {
			infoPanel.clearReference();
		}
	}

	private void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (reference == null) {
			infoPanel.clearReference();
			return;
		}

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
		label.setPreferredSize(ScaleUtil.getDimension(100, ScaleUtil.invert(label.getPreferredSize().height)));
		panel.add(label);

		panel.add(GuiUtil.unboldLabel(new JLabel(value, JLabel.LEFT)));

		container.add(panel);
	}

	private JComboBox<AccessModifier> addModifierComboBox(JPanel container, String name, Entry<?> entry) {
		if (!getController().project.isRenamable(entry))
			return null;
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
		JLabel label = new JLabel(name + ":", JLabel.RIGHT);
		label.setPreferredSize(ScaleUtil.getDimension(100, ScaleUtil.invert(label.getPreferredSize().height)));
		panel.add(label);
		JComboBox<AccessModifier> combo = new JComboBox<>(AccessModifier.values());
		((JLabel) combo.getRenderer()).setHorizontalAlignment(JLabel.LEFT);
		combo.setPreferredSize(ScaleUtil.getDimension(100, ScaleUtil.invert(label.getPreferredSize().height)));

		EntryMapping mapping = controller.project.getMapper().getDeobfMapping(entry);
		if (mapping != null) {
			combo.setSelectedIndex(mapping.getAccessModifier().ordinal());
		} else {
			combo.setSelectedIndex(AccessModifier.UNCHANGED.ordinal());
		}

		combo.addItemListener(event -> {
			EntryReference<Entry<?>, Entry<?>> cursorReference = getCursorReference();
			if (cursorReference == null) return;

			if (event.getStateChange() == ItemEvent.SELECTED) {
				Entry<?> e = cursorReference.entry;
				AccessModifier modifier = (AccessModifier) event.getItem();
				getController().onModifierChanged(e, modifier);
			}
		});

		panel.add(combo);

		container.add(panel);

		return combo;
	}

	@Nullable
	public PanelEditor getActiveEditor() {
		return editors.values().stream()
				.filter(e -> e.getUi() == openFiles.getSelectedComponent())
				.findFirst()
				.orElse(null);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		PanelEditor activeEditor = getActiveEditor();
		return activeEditor == null ? null : activeEditor.getCursorReference();
	}

	public void startDocChange(PanelEditor editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;
		JavadocDialog.show(frame, getController(), cursorReference);
	}

	public void startRename(PanelEditor editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		// init the text box
		renameTextField = new JTextField();

		EntryReference<Entry<?>, Entry<?>> translatedReference = controller.project.getMapper().deobfuscate(cursorReference);
		renameTextField.setText(translatedReference.getNameableName());

		renameTextField.setPreferredSize(ScaleUtil.getDimension(360, ScaleUtil.invert(renameTextField.getPreferredSize().height)));
		renameTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						finishRename(cursorReference, true);
						break;

					case KeyEvent.VK_ESCAPE:
						finishRename(cursorReference, false);
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

		renamingReference = cursorReference;

		redraw();
	}

	private void finishRename(EntryReference<Entry<?>, Entry<?>> cursorReference, boolean saveName) {
		String newName = renameTextField.getText();

		if (saveName && newName != null && !newName.isEmpty()) {
			try {
				this.controller.rename(renamingReference, newName, true);
				this.controller.sendPacket(new RenameC2SPacket(renamingReference.getNameableEntry(), newName, true));
				renameTextField = null;
			} catch (IllegalNameException ex) {
				renameTextField.setBorder(BorderFactory.createLineBorder(Color.red, 1));
				renameTextField.setToolTipText(ex.getReason());
				GuiUtil.showToolTipNow(renameTextField);
			}
			return;
		}

		renameTextField = null;

		// abort the rename
		showCursorReference(cursorReference);

		redraw();
	}

	private boolean isRenaming() {
		return renameTextField != null;
	}

	public void showInheritance(PanelEditor editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

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

	public void showImplementations(PanelEditor editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

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

	public void showCalls(PanelEditor editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

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

	public void toggleMapping(PanelEditor editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

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
			exit();
		} else {
			// ask to save before closing
			showDiscardDiag((response) -> {
				if (response == JOptionPane.YES_OPTION) {
					this.saveMapping();
					exit();
				} else if (response == JOptionPane.NO_OPTION) {
					exit();
				}

				return null;
			}, I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.close.cancel"));
		}
	}

	private void exit() {
		if (searchDialog != null) {
			searchDialog.dispose();
		}
		this.frame.dispose();
		System.exit(0);
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

		List<ClassSelector.StateEntry> stateDeobf = this.deobfPanel.deobfClasses.getExpansionState(this.deobfPanel.deobfClasses);
		List<ClassSelector.StateEntry> stateObf = this.obfPanel.obfClasses.getExpansionState(this.obfPanel.obfClasses);

		// Ob -> deob
		if (!isNewOb) {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.moveClassOut(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Deob -> ob
		else if (!isOldOb) {
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

	public PanelObf getObfPanel() {
		return obfPanel;
	}

	public PanelDeobf getDeobfPanel() {
		return deobfPanel;
	}

	public SearchDialog getSearchDialog() {
		if (searchDialog == null) {
			searchDialog = new SearchDialog(this);
		}
		return searchDialog;
	}


	public MenuBar getMenuBar() {
		return menuBar;
	}

	public void addMessage(Message message) {
		JScrollBar verticalScrollBar = messageScrollPane.getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		messageModel.addElement(message);
		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}
		statusLabel.setText(message.translate());
	}

	public void setUserList(List<String> users) {
		userModel.clear();
		users.forEach(userModel::addElement);
		connectionStatusLabel.setText(String.format(I18n.translate("status.connected_user_count"), users.size()));
	}

	private void sendMessage() {
		String text = chatBox.getText().trim();
		if (!text.isEmpty()) {
			getController().sendPacket(new MessageC2SPacket(text));
		}
		chatBox.setText("");
	}

	/**
	 * Updates the state of the UI elements (button text, enabled state, ...) to reflect the current program state.
	 * This is a central place to update the UI state to prevent multiple code paths from changing the same state,
	 * causing inconsistencies.
	 */
	public void updateUiState() {
		menuBar.updateUiState();

		connectionStatusLabel.setText(I18n.translate(connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));

		if (connectionState == ConnectionState.NOT_CONNECTED) {
			logSplit.setLeftComponent(null);
			splitRight.setRightComponent(tabs);
		} else {
			splitRight.setRightComponent(logSplit);
			logSplit.setLeftComponent(tabs);
		}
	}

	public void setConnectionState(ConnectionState state) {
		connectionState = state;
		statusLabel.setText(I18n.translate("status.ready"));
		updateUiState();
	}

	public boolean isJarOpen() {
		return isJarOpen;
	}

	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

}
