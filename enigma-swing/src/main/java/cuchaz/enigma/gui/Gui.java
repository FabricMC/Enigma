/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.elements.CallsTree;
import cuchaz.enigma.gui.elements.CollapsibleTabbedPane;
import cuchaz.enigma.gui.elements.EditorTabbedPane;
import cuchaz.enigma.gui.elements.ImplementationsTree;
import cuchaz.enigma.gui.elements.InheritanceTree;
import cuchaz.enigma.gui.elements.MainWindow;
import cuchaz.enigma.gui.elements.MenuBar;
import cuchaz.enigma.gui.elements.ValidatableUi;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.gui.panels.DeobfPanel;
import cuchaz.enigma.gui.panels.EditorPanel;
import cuchaz.enigma.gui.panels.IdentifierPanel;
import cuchaz.enigma.gui.panels.ObfPanel;
import cuchaz.enigma.gui.panels.StructurePanel;
import cuchaz.enigma.gui.renderer.MessageListCellRenderer;
import cuchaz.enigma.gui.util.ExtensionFileFilter;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

public class Gui {
	private final MainWindow mainWindow = new MainWindow(Enigma.NAME);
	private final GuiController controller;

	private ConnectionState connectionState;
	private boolean isJarOpen;
	private final Set<EditableType> editableTypes;
	private boolean singleClassTree;

	private final MenuBar menuBar;
	private final ObfPanel obfPanel;
	private final DeobfPanel deobfPanel;
	private final IdentifierPanel infoPanel;
	private final StructurePanel structurePanel;
	private final InheritanceTree inheritanceTree;
	private final ImplementationsTree implementationsTree;
	private final CallsTree callsTree;

	private final EditorTabbedPane editorTabbedPane;

	private final JPanel classesPanel = new JPanel(new BorderLayout());
	private final JSplitPane splitClasses;
	private final JTabbedPane tabs = new JTabbedPane();
	private final CollapsibleTabbedPane logTabs = new CollapsibleTabbedPane(JTabbedPane.BOTTOM);
	private final JSplitPane logSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabs, logTabs);
	private final JPanel centerPanel = new JPanel(new BorderLayout());
	private final JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, this.logSplit);
	private final JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.classesPanel, splitRight);

	private final DefaultListModel<String> userModel = new DefaultListModel<>();
	private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
	private final JList<String> users = new JList<>(userModel);
	private final JList<Message> messages = new JList<>(messageModel);
	private final JPanel messagePanel = new JPanel(new BorderLayout());
	private final JScrollPane messageScrollPane = new JScrollPane(this.messages);
	private final JTextField chatBox = new JTextField();

	private final JLabel connectionStatusLabel = new JLabel();

	public final JFileChooser jarFileChooser = new JFileChooser();
	public final JFileChooser mappingsFileChooser = new JFileChooser();
	public final JFileChooser exportSourceFileChooser = new JFileChooser();
	public final JFileChooser exportJarFileChooser = new JFileChooser();
	public SearchDialog searchDialog;

	public Gui(EnigmaProfile profile, Set<EditableType> editableTypes) {
		this.editableTypes = editableTypes;
		this.controller = new GuiController(this, profile);
		this.structurePanel = new StructurePanel(this);
		this.deobfPanel = new DeobfPanel(this);
		this.infoPanel = new IdentifierPanel(this);
		this.obfPanel = new ObfPanel(this);
		this.menuBar = new MenuBar(this);
		this.inheritanceTree = new InheritanceTree(this);
		this.implementationsTree = new ImplementationsTree(this);
		this.callsTree = new CallsTree(this);
		this.editorTabbedPane = new EditorTabbedPane(this);
		this.splitClasses = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.obfPanel, this.deobfPanel);

		this.setupUi();

		LanguageUtil.addListener(this::retranslateUi);
		Themes.addListener((lookAndFeel, boxHighlightPainters) -> SwingUtilities.updateComponentTreeUI(this.getFrame()));

		this.mainWindow.setVisible(true);
	}

	private void setupUi() {
		this.jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		this.exportSourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		this.exportSourceFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportJarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		this.splitClasses.setResizeWeight(0.3);
		this.classesPanel.setPreferredSize(ScaleUtil.getDimension(250, 0));

		// layout controls
		Container workArea = this.mainWindow.workArea();
		workArea.setLayout(new BorderLayout());

		centerPanel.add(infoPanel.getUi(), BorderLayout.NORTH);
		centerPanel.add(this.editorTabbedPane.getUi(), BorderLayout.CENTER);

		tabs.setPreferredSize(ScaleUtil.getDimension(250, 0));
		tabs.addTab(I18n.translate("info_panel.tree.structure"), structurePanel.getPanel());
		tabs.addTab(I18n.translate("info_panel.tree.inheritance"), inheritanceTree.getPanel());
		tabs.addTab(I18n.translate("info_panel.tree.implementations"), implementationsTree.getPanel());
		tabs.addTab(I18n.translate("info_panel.tree.calls"), callsTree.getPanel());

		messages.setCellRenderer(new MessageListCellRenderer());
		JPanel chatPanel = new JPanel(new BorderLayout());
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
		messagePanel.add(messageScrollPane, BorderLayout.CENTER);
		messagePanel.add(chatPanel, BorderLayout.SOUTH);
		logTabs.addTab(I18n.translate("log_panel.users"), new JScrollPane(this.users));
		logTabs.addTab(I18n.translate("log_panel.messages"), messagePanel);
		logSplit.setResizeWeight(0.5);
		logSplit.resetToPreferredSizes();
		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		splitCenter.setResizeWeight(0); // let the right side take all the slack

		workArea.add(splitCenter, BorderLayout.CENTER);

		// restore state
		int[] layout = UiConfig.getLayout();

		if (layout.length >= 4) {
			this.splitClasses.setDividerLocation(layout[0]);
			this.splitCenter.setDividerLocation(layout[1]);
			this.splitRight.setDividerLocation(layout[2]);
			this.logSplit.setDividerLocation(layout[3]);
		}

		this.mainWindow.statusBar().addPermanentComponent(this.connectionStatusLabel);

		// init state
		setConnectionState(ConnectionState.NOT_CONNECTED);
		onCloseJar();

		JFrame frame = this.mainWindow.frame();
		frame.addWindowListener(GuiUtil.onWindowClose(e -> this.close()));

		frame.setSize(UiConfig.getWindowSize("Main Window", ScaleUtil.getDimension(1024, 576)));
		frame.setExtendedState(UiConfig.isFullscreen("Main Window") ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
		frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Point windowPos = UiConfig.getWindowPos("Main Window", null);

		if (windowPos != null) {
			frame.setLocation(windowPos);
		} else {
			frame.setLocationRelativeTo(null);
		}

		this.retranslateUi();
	}

	public MainWindow getMainWindow() {
		return this.mainWindow;
	}

	public JFrame getFrame() {
		return this.mainWindow.frame();
	}

	public GuiController getController() {
		return this.controller;
	}

	public void setSingleClassTree(boolean singleClassTree) {
		this.singleClassTree = singleClassTree;
		this.classesPanel.removeAll();
		this.classesPanel.add(isSingleClassTree() ? deobfPanel : splitClasses);
		getController().refreshClasses();
		retranslateUi();
	}

	public boolean isSingleClassTree() {
		return singleClassTree;
	}

	public void onStartOpenJar() {
		this.classesPanel.removeAll();
		redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME + " - " + jarName);
		this.classesPanel.removeAll();
		this.classesPanel.add(isSingleClassTree() ? deobfPanel : splitClasses);
		this.editorTabbedPane.closeAllEditorTabs();

		// update menu
		isJarOpen = true;

		updateUiState();
		redraw();
	}

	public void onCloseJar() {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		this.editorTabbedPane.closeAllEditorTabs();
		this.classesPanel.removeAll();

		// update menu
		isJarOpen = false;
		setMappingsFile(null);

		updateUiState();
		redraw();
	}

	public EditorPanel openClass(ClassEntry entry) {
		return this.editorTabbedPane.openClass(entry);
	}

	@Nullable
	public EditorPanel getActiveEditor() {
		return this.editorTabbedPane.getActiveEditor();
	}

	public void closeEditor(EditorPanel editor) {
		this.editorTabbedPane.closeEditor(editor);
	}

	/**
	 * Navigates to the reference without modifying history. If the class is not currently loaded, it will be loaded.
	 *
	 * @param reference the reference
	 */
	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		this.editorTabbedPane.openClass(reference.getLocationClassEntry().getOutermostClass()).showReference(reference);
	}

	public void setObfClasses(Collection<ClassEntry> obfClasses) {
		this.obfPanel.obfClasses.setClasses(obfClasses);
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		this.deobfPanel.deobfClasses.setClasses(deobfClasses);
	}

	public void setMappingsFile(Path path) {
		this.mappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		updateUiState();
	}

	public void showTokens(EditorPanel editor, List<Token> tokens) {
		if (tokens.size() > 1) {
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			this.callsTree.showTokens(tokens);
		} else {
			this.callsTree.clearTokens();
		}

		// show the first token
		editor.navigateToToken(tokens.get(0));
	}

	public void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		infoPanel.setReference(reference == null ? null : reference.entry);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		EditorPanel activeEditor = this.editorTabbedPane.getActiveEditor();
		return activeEditor == null ? null : activeEditor.getCursorReference();
	}

	public void startDocChange(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();

		if (cursorReference == null || !this.isEditable(EditableType.JAVADOC)) {
			return;
		}

		JavadocDialog.show(mainWindow.frame(), getController(), cursorReference);
	}

	public void startRename(EditorPanel editor, String text) {
		if (editor != this.editorTabbedPane.getActiveEditor()) {
			return;
		}

		infoPanel.startRenaming(text);
	}

	public void startRename(EditorPanel editor) {
		if (editor != this.editorTabbedPane.getActiveEditor()) {
			return;
		}

		infoPanel.startRenaming();
	}

	public void showStructure(EditorPanel editor) {
		this.structurePanel.showStructure(editor);
	}

	public void showInheritance(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();

		if (cursorReference == null) {
			return;
		}

		this.inheritanceTree.display(cursorReference.entry);
		tabs.setSelectedIndex(1);
	}

	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();

		if (cursorReference == null) {
			return;
		}

		this.implementationsTree.display(cursorReference.entry);
		tabs.setSelectedIndex(2);
	}

	public void showCalls(EditorPanel editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();

		if (cursorReference == null) {
			return;
		}

		this.callsTree.showCalls(cursorReference.entry, recurse);
		tabs.setSelectedIndex(3);
	}

	public void toggleMapping(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();

		if (cursorReference == null) {
			return;
		}

		Entry<?> obfEntry = cursorReference.getNameableEntry();
		toggleMappingFromEntry(obfEntry);
	}

	public void toggleMappingFromEntry(Entry<?> obfEntry) {
		if (this.controller.project.getMapper().getDeobfMapping(obfEntry).targetName() != null) {
			validateImmediateAction(vc -> this.controller.applyChange(vc, EntryChange.modify(obfEntry).clearDeobfName()));
		} else {
			validateImmediateAction(vc -> this.controller.applyChange(vc, EntryChange.modify(obfEntry).withDefaultDeobfName(this.getController().project)));
		}
	}

	public void showDiscardDiag(Function<Integer, Void> callback, String... options) {
		int response = JOptionPane.showOptionDialog(this.mainWindow.frame(), I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public CompletableFuture<Void> saveMapping() {
		ExtensionFileFilter.setupFileChooser(this.mappingsFileChooser, this.controller.getLoadedMappingFormat());

		if (this.mappingsFileChooser.getSelectedFile() != null || this.mappingsFileChooser.showSaveDialog(this.mainWindow.frame()) == JFileChooser.APPROVE_OPTION) {
			return this.controller.saveMappings(ExtensionFileFilter.getSavePath(this.mappingsFileChooser));
		}

		return CompletableFuture.completedFuture(null);
	}

	public void close() {
		if (!this.controller.isDirty()) {
			// everything is saved, we can exit safely
			exit();
		} else {
			// ask to save before closing
			showDiscardDiag((response) -> {
				if (response == JOptionPane.YES_OPTION) {
					this.saveMapping().thenRun(this::exit);
					// do not join, as join waits on swing to clear events
				} else if (response == JOptionPane.NO_OPTION) {
					exit();
				}

				return null;
			}, I18n.translate("prompt.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		}
	}

	private void exit() {
		UiConfig.setWindowPos("Main Window", this.mainWindow.frame().getLocationOnScreen());
		UiConfig.setWindowSize("Main Window", this.mainWindow.frame().getSize());
		UiConfig.setFullscreen("Main Window", this.mainWindow.frame().getExtendedState() == JFrame.MAXIMIZED_BOTH);
		UiConfig.setLayout(this.splitClasses.getDividerLocation(), this.splitCenter.getDividerLocation(), this.splitRight.getDividerLocation(), this.logSplit.getDividerLocation());
		UiConfig.save();

		if (searchDialog != null) {
			searchDialog.dispose();
		}

		this.mainWindow.frame().dispose();
		System.exit(0);
	}

	public void redraw() {
		JFrame frame = this.mainWindow.frame();

		frame.validate();
		frame.repaint();
	}

	public void onRenameFromClassTree(ValidationContext vc, String targetName, DefaultMutableTreeNode node) {
		List<EntryChange<ClassEntry>> task = new ArrayList<>();
		onRenameFromClassTree(targetName, node, task);
		this.controller.applyChanges(vc, task);
	}

	private void onRenameFromClassTree(String targetName, DefaultMutableTreeNode node, List<EntryChange<ClassEntry>> entryOps) {
		if (targetName.startsWith("/")) {
			targetName = targetName.substring(1);
		}

		if (node instanceof ClassSelectorPackageNode) {
			for (int i = 0; i < node.getChildCount(); i++) {
				String childName;
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);

				if (child instanceof ClassSelectorPackageNode packageNode) {
					String[] parts = packageNode.getPackageName().split("/");
					childName = targetName + "/" + parts[parts.length - 1];
				} else if (child instanceof ClassSelectorClassNode classNode) {
					childName = targetName + "/" + classNode.getClassEntry().getSimpleName();
				} else {
					throw new IllegalStateException(String.format("unhandled rename object type: '%s'",
							child.getClass()));
				}

				onRenameFromClassTree(childName, child, entryOps);
			}
		} else if (node instanceof ClassSelectorClassNode classSelectorClassNode) {
			EntryRemapper mapper = this.controller.project.getMapper();
			ClassEntry deobf = classSelectorClassNode.getClassEntry();
			ClassEntry obf = mapper.getObfToDeobf().getAllEntries()
					.filter(e -> e instanceof ClassEntry)
					.map(e -> (ClassEntry) e)
					.filter(e -> mapper.deobfuscate(e).equals(deobf))
					.findAny()
					.orElse(deobf);

			entryOps.add(EntryChange.modify(obf).withDeobfName(targetName));
		} else {
			throw new IllegalStateException(String.format("unhandled rename object type: '%s'", node.getClass()));
		}
	}

	public void moveClassTree(Entry<?> obfEntry, String newName) {
		String oldEntry = obfEntry.getContainingClass().getPackageName();
		String newEntry = new ClassEntry(newName).getPackageName();
		moveClassTree(obfEntry, oldEntry == null, newEntry == null);
	}

	// TODO: getExpansionState will *not* actually update itself based on name changes!
	public void moveClassTree(Entry<?> obfEntry, boolean isOldOb, boolean isNewOb) {
		ClassEntry classEntry = obfEntry.getContainingClass();

		List<ClassSelector.StateEntry> stateDeobf = this.deobfPanel.deobfClasses.getExpansionState();
		List<ClassSelector.StateEntry> stateObf = this.obfPanel.obfClasses.getExpansionState();

		// Ob -> deob
		if (!isNewOb) {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.removeEntry(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		} else if (!isOldOb) { // Deob -> ob
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.removeEntry(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		} else if (isOldOb) { // Local move
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.reload();
		} else {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.reload();
		}

		this.deobfPanel.deobfClasses.restoreExpansionState(stateDeobf);
		this.obfPanel.obfClasses.restoreExpansionState(stateObf);
	}

	public ObfPanel getObfPanel() {
		return obfPanel;
	}

	public DeobfPanel getDeobfPanel() {
		return deobfPanel;
	}

	public SearchDialog getSearchDialog() {
		if (searchDialog == null) {
			searchDialog = new SearchDialog(this);
		}

		return searchDialog;
	}

	public void addMessage(Message message) {
		JScrollBar verticalScrollBar = messageScrollPane.getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		messageModel.addElement(message);

		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}

		this.mainWindow.statusBar().showMessage(message.translate(), 5000);
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

		splitRight.setDividerLocation(splitRight.getDividerLocation());
	}

	public void retranslateUi() {
		this.jarFileChooser.setDialogTitle(I18n.translate("menu.file.jar.open"));
		this.exportJarFileChooser.setDialogTitle(I18n.translate("menu.file.export.jar"));
		this.tabs.setTitleAt(0, I18n.translate("info_panel.tree.structure"));
		this.tabs.setTitleAt(1, I18n.translate("info_panel.tree.inheritance"));
		this.tabs.setTitleAt(2, I18n.translate("info_panel.tree.implementations"));
		this.tabs.setTitleAt(3, I18n.translate("info_panel.tree.calls"));
		this.logTabs.setTitleAt(0, I18n.translate("log_panel.users"));
		this.logTabs.setTitleAt(1, I18n.translate("log_panel.messages"));
		this.connectionStatusLabel.setText(I18n.translate(connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));

		this.updateUiState();

		this.menuBar.retranslateUi();
		this.obfPanel.retranslateUi();
		this.deobfPanel.retranslateUi();
		this.infoPanel.retranslateUi();
		this.structurePanel.retranslateUi();
		this.editorTabbedPane.retranslateUi();
		this.inheritanceTree.retranslateUi();
		this.implementationsTree.retranslateUi();
		this.structurePanel.retranslateUi();
		this.callsTree.retranslateUi();
	}

	public void setConnectionState(ConnectionState state) {
		connectionState = state;
		updateUiState();
	}

	public boolean isJarOpen() {
		return isJarOpen;
	}

	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

	public boolean validateImmediateAction(Consumer<ValidationContext> op) {
		ValidationContext vc = new ValidationContext();
		op.accept(vc);

		if (!vc.canProceed()) {
			List<ParameterizedMessage> messages = vc.getMessages();
			String text = ValidatableUi.formatMessages(messages);
			JOptionPane.showMessageDialog(this.getFrame(), text, String.format("%d message(s)", messages.size()), JOptionPane.ERROR_MESSAGE);
		}

		return vc.canProceed();
	}

	public boolean isEditable(EditableType t) {
		return this.editableTypes.contains(t);
	}
}
