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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.Constants;
import cuchaz.enigma.ExceptionIgnorer;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.gui.dialog.CrashDialog;
import cuchaz.enigma.gui.elements.MenuBar;
import cuchaz.enigma.gui.elements.PopupMenuBar;
import cuchaz.enigma.gui.filechooser.FileChooserFile;
import cuchaz.enigma.gui.filechooser.FileChooserFolder;
import cuchaz.enigma.gui.highlight.DeobfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.ObfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.OtherHighlightPainter;
import cuchaz.enigma.gui.highlight.SelectionHighlightPainter;
import cuchaz.enigma.gui.panels.PanelDeobf;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.gui.panels.PanelIdentifier;
import cuchaz.enigma.gui.panels.PanelObf;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.utils.Utils;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class Gui {

    private GuiController controller;

    private final PanelObf obfPanel;
    private final PanelDeobf deobfPanel;

    private final MenuBar menuBar;
    public final PopupMenuBar popupMenu;

    private JFrame frame;
    private PanelEditor editor;
    private JPanel classesPanel;
    private JSplitPane m_splitClasses;
    private PanelIdentifier m_infoPanel;
    private ObfuscatedHighlightPainter m_obfuscatedHighlightPainter;
    private DeobfuscatedHighlightPainter m_deobfuscatedHighlightPainter;
    private OtherHighlightPainter m_otherHighlightPainter;
    private SelectionHighlightPainter m_selectionHighlightPainter;
    private JTree m_inheritanceTree;
    private JTree m_implementationsTree;
    private JTree m_callsTree;
    private JList<Token> m_tokens;
    private JTabbedPane m_tabs;

    // state
    public EntryReference<Entry, Entry> m_reference;

    public JFileChooser jarFileChooser;
    public JFileChooser mappingsFileChooser;
    public JFileChooser oldMappingsFileChooser;

    public JFileChooser exportSourceFileChooser;
    public JFileChooser exportJarFileChooser;

    public Gui() {

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

        this.controller = new GuiController(this);

        // init file choosers
        this.jarFileChooser = new FileChooserFile();
        this.mappingsFileChooser = new FileChooserFolder();


        this.oldMappingsFileChooser = new FileChooserFile();
        this.exportSourceFileChooser = new FileChooserFolder();
        this.exportJarFileChooser = new FileChooserFile();

        this.obfPanel = new PanelObf(this);
        this.deobfPanel = new PanelDeobf(this);

        // set up classes panel (don't add the splitter yet)
        m_splitClasses = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.obfPanel, this.deobfPanel);
        m_splitClasses.setResizeWeight(0.3);
        this.classesPanel = new JPanel();
        this.classesPanel.setLayout(new BorderLayout());
        this.classesPanel.setPreferredSize(new Dimension(250, 0));

        // init info panel
        m_infoPanel = new PanelIdentifier(this);
        m_infoPanel.clearReference();

        // init editor
        DefaultSyntaxKit.initKit();
        m_obfuscatedHighlightPainter = new ObfuscatedHighlightPainter();
        m_deobfuscatedHighlightPainter = new DeobfuscatedHighlightPainter();
        m_otherHighlightPainter = new OtherHighlightPainter();
        m_selectionHighlightPainter = new SelectionHighlightPainter();
        this.editor = new PanelEditor(this);
        JScrollPane sourceScroller = new JScrollPane(this.editor);

        // init editor popup menu
        this.popupMenu = new PopupMenuBar(this);
        this.editor.setComponentPopupMenu(this.popupMenu);

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
                        ReferenceTreeNode<Entry, Entry> referenceNode = ((ReferenceTreeNode<Entry, Entry>) node);
                        if (referenceNode.getReference() != null) {
                            navigateTo(referenceNode.getReference());
                        } else {
                            navigateTo(referenceNode.getEntry());
                        }
                    }
                }
            }
        });
        m_tokens = new JList<>();
        m_tokens.setCellRenderer(new TokenListCellRenderer(this.controller));
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

    public void onStartOpenJar() {
        this.classesPanel.removeAll();
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Loading..."));
        this.classesPanel.add(panel);
        redraw();
    }

    public void onFinishOpenJar(String jarName) {
        // update gui
        this.frame.setTitle(Constants.NAME + " - " + jarName);
        this.classesPanel.removeAll();
        this.classesPanel.add(m_splitClasses);
        setSource(null);

        // update menu
        this.menuBar.closeJarMenu.setEnabled(true);
        this.menuBar.openOldMappingsMenu.setEnabled(true);
        this.menuBar.openMappingsMenu.setEnabled(true);
        this.menuBar.saveMappingsMenu.setEnabled(false);
        this.menuBar.saveMappingsAsMenu.setEnabled(true);
        this.menuBar.saveMappingsOldMenu.setEnabled(true);
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
        this.menuBar.openOldMappingsMenu.setEnabled(false);
        this.menuBar.openMappingsMenu.setEnabled(false);
        this.menuBar.saveMappingsMenu.setEnabled(false);
        this.menuBar.saveMappingsAsMenu.setEnabled(false);
        this.menuBar.saveMappingsOldMenu.setEnabled(false);
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

    public void setMappingsFile(File file) {
        this.mappingsFileChooser.setSelectedFile(file);
        this.menuBar.saveMappingsMenu.setEnabled(file != null);
    }

    public void setSource(String source) {
        this.editor.getHighlighter().removeAllHighlights();
        this.editor.setText(source);
    }

    public void showToken(final Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null!");
        }
        Utils.navigateToToken(this.editor, token, m_selectionHighlightPainter);
        redraw();
    }

    public void showTokens(Collection<Token> tokens) {
        Vector<Token> sortedTokens = new Vector<>(tokens);
        Collections.sort(sortedTokens);
        if (sortedTokens.size() > 1) {
            // sort the tokens and update the tokens panel
            m_tokens.setListData(sortedTokens);
            m_tokens.setSelectedIndex(0);
        } else {
            m_tokens.setListData(new Vector<>());
        }

        // show the first token
        showToken(sortedTokens.get(0));
    }

    public void setHighlightedTokens(Iterable<Token> obfuscatedTokens, Iterable<Token> deobfuscatedTokens, Iterable<Token> otherTokens) {

        // remove any old highlighters
        this.editor.getHighlighter().removeAllHighlights();

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
                this.editor.getHighlighter().addHighlight(token.start, token.end, painter);
            } catch (BadLocationException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    private void showReference(EntryReference<Entry, Entry> reference) {
        if (reference == null) {
            m_infoPanel.clearReference();
            return;
        }

        m_reference = reference;

        m_infoPanel.removeAll();
        if (reference.entry instanceof ClassEntry) {
            showClassEntry((ClassEntry) m_reference.entry);
        } else if (m_reference.entry instanceof FieldEntry) {
            showFieldEntry((FieldEntry) m_reference.entry);
        } else if (m_reference.entry instanceof MethodEntry) {
            showMethodEntry((MethodEntry) m_reference.entry);
        } else if (m_reference.entry instanceof ConstructorEntry) {
            showConstructorEntry((ConstructorEntry) m_reference.entry);
        } else if (m_reference.entry instanceof ArgumentEntry) {
            showArgumentEntry((ArgumentEntry) m_reference.entry);
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

        panel.add(Utils.unboldLabel(new JLabel(value, JLabel.LEFT)));
    }

    public void onCaretMove(int pos) {

        Token token = this.controller.getToken(pos);
        boolean isToken = token != null;

        m_reference = this.controller.getDeobfReference(token);
        boolean isClassEntry = isToken && m_reference.entry instanceof ClassEntry;
        boolean isFieldEntry = isToken && m_reference.entry instanceof FieldEntry;
        boolean isMethodEntry = isToken && m_reference.entry instanceof MethodEntry;
        boolean isConstructorEntry = isToken && m_reference.entry instanceof ConstructorEntry;
        boolean isInJar = isToken && this.controller.entryIsInJar(m_reference.entry);
        boolean isRenameable = isToken && this.controller.referenceIsRenameable(m_reference);

        if (isToken) {
            showReference(m_reference);
        } else {
            m_infoPanel.clearReference();
        }

        this.popupMenu.renameMenu.setEnabled(isRenameable);
        this.popupMenu.showInheritanceMenu.setEnabled(isClassEntry || isMethodEntry || isConstructorEntry);
        this.popupMenu.showImplementationsMenu.setEnabled(isClassEntry || isMethodEntry);
        this.popupMenu.showCallsMenu.setEnabled(isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry);
        this.popupMenu.openEntryMenu.setEnabled(isInJar && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
        this.popupMenu.openPreviousMenu.setEnabled(this.controller.hasPreviousLocation());
        this.popupMenu.toggleMappingMenu.setEnabled(isRenameable);

        if (isToken && this.controller.entryHasDeobfuscatedName(m_reference.entry)) {
            this.popupMenu.toggleMappingMenu.setText("Reset to obfuscated");
        } else {
            this.popupMenu.toggleMappingMenu.setText("Mark as deobfuscated");
        }
    }

    public void navigateTo(Entry entry) {
        if (!this.controller.entryIsInJar(entry)) {
            // entry is not in the jar. Ignore it
            return;
        }
        if (m_reference != null) {
            this.controller.savePreviousReference(m_reference);
        }
        this.controller.openDeclaration(entry);
    }

    private void navigateTo(EntryReference<Entry, Entry> reference) {
        if (!this.controller.entryIsInJar(reference.getLocationClassEntry())) {
            return;
        }
        if (m_reference != null) {
            this.controller.savePreviousReference(m_reference);
        }
        this.controller.openReference(reference);
    }

    public void startRename() {

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
        JPanel panel = (JPanel) m_infoPanel.getComponent(0);
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
                this.controller.rename(m_reference, newName);
            } catch (IllegalNameException ex) {
                text.setBorder(BorderFactory.createLineBorder(Color.red, 1));
                text.setToolTipText(ex.getReason());
                Utils.showToolTipNow(text);
            }
            return;
        }

        // abort the rename
        JPanel panel = (JPanel) m_infoPanel.getComponent(0);
        panel.remove(panel.getComponentCount() - 1);
        panel.add(Utils.unboldLabel(new JLabel(m_reference.getNamableName(), JLabel.LEFT)));

        this.editor.grabFocus();

        redraw();
    }

    public void showInheritance() {

        if (m_reference == null) {
            return;
        }

        m_inheritanceTree.setModel(null);

        if (m_reference.entry instanceof ClassEntry) {
            // get the class inheritance
            ClassInheritanceTreeNode classNode = this.controller.getClassInheritance((ClassEntry) m_reference.entry);

            // show the tree at the root
            TreePath path = getPathToRoot(classNode);
            m_inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
            m_inheritanceTree.expandPath(path);
            m_inheritanceTree.setSelectionRow(m_inheritanceTree.getRowForPath(path));
        } else if (m_reference.entry instanceof MethodEntry) {
            // get the method inheritance
            MethodInheritanceTreeNode classNode = this.controller.getMethodInheritance((MethodEntry) m_reference.entry);

            // show the tree at the root
            TreePath path = getPathToRoot(classNode);
            m_inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
            m_inheritanceTree.expandPath(path);
            m_inheritanceTree.setSelectionRow(m_inheritanceTree.getRowForPath(path));
        }

        m_tabs.setSelectedIndex(0);
        redraw();
    }

    public void showImplementations() {

        if (m_reference == null) {
            return;
        }

        m_implementationsTree.setModel(null);

        if (m_reference.entry instanceof ClassEntry) {
            // get the class implementations
            ClassImplementationsTreeNode node = this.controller.getClassImplementations((ClassEntry) m_reference.entry);
            if (node != null) {
                // show the tree at the root
                TreePath path = getPathToRoot(node);
                m_implementationsTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
                m_implementationsTree.expandPath(path);
                m_implementationsTree.setSelectionRow(m_implementationsTree.getRowForPath(path));
            }
        } else if (m_reference.entry instanceof MethodEntry) {
            // get the method implementations
            MethodImplementationsTreeNode node = this.controller.getMethodImplementations((MethodEntry) m_reference.entry);
            if (node != null) {
                // show the tree at the root
                TreePath path = getPathToRoot(node);
                m_implementationsTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
                m_implementationsTree.expandPath(path);
                m_implementationsTree.setSelectionRow(m_implementationsTree.getRowForPath(path));
            }
        }

        m_tabs.setSelectedIndex(1);
        redraw();
    }

    public void showCalls() {

        if (m_reference == null) {
            return;
        }

        if (m_reference.entry instanceof ClassEntry) {
            // look for calls to the default constructor
            // TODO: get a list of all the constructors and find calls to all of them
            BehaviorReferenceTreeNode node = this.controller.getMethodReferences(new ConstructorEntry((ClassEntry) m_reference.entry, new Signature("()V")));
            m_callsTree.setModel(new DefaultTreeModel(node));
        } else if (m_reference.entry instanceof FieldEntry) {
            FieldReferenceTreeNode node = this.controller.getFieldReferences((FieldEntry) m_reference.entry);
            m_callsTree.setModel(new DefaultTreeModel(node));
        } else if (m_reference.entry instanceof MethodEntry) {
            BehaviorReferenceTreeNode node = this.controller.getMethodReferences((MethodEntry) m_reference.entry);
            m_callsTree.setModel(new DefaultTreeModel(node));
        } else if (m_reference.entry instanceof ConstructorEntry) {
            BehaviorReferenceTreeNode node = this.controller.getMethodReferences((ConstructorEntry) m_reference.entry);
            m_callsTree.setModel(new DefaultTreeModel(node));
        }

        m_tabs.setSelectedIndex(2);
        redraw();
    }

    public void toggleMapping() {
        if (this.controller.entryHasDeobfuscatedName(m_reference.entry)) {
            this.controller.removeMapping(m_reference);
        } else {
            this.controller.markAsDeobfuscated(m_reference);
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

    public void close() {
        if (!this.controller.isDirty()) {
            // everything is saved, we can exit safely
            this.frame.dispose();
            System.exit(0);
        } else {
            // ask to save before closing
            String[] options = {"Save and exit", "Discard changes", "Cancel"};
            int response = JOptionPane.showOptionDialog(this.frame, "Your mappings have not been saved yet. Do you want to save?", "Save your changes?", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
            switch (response) {
                case JOptionPane.YES_OPTION: // save and exit
                    if (this.mappingsFileChooser.getSelectedFile() != null || this.mappingsFileChooser.showSaveDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
                        try {
                            this.controller.saveMappings(this.mappingsFileChooser.getCurrentDirectory());
                            this.frame.dispose();
                        } catch (IOException ex) {
                            throw new Error(ex);
                        }
                    }
                    break;

                case JOptionPane.NO_OPTION:
                    // don't save, exit
                    this.frame.dispose();
                    break;

                // cancel means do nothing
            }
        }
    }

    public void redraw() {
        this.frame.validate();
        this.frame.repaint();
    }
}
