package cuchaz.enigma.gui.panels;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import de.sciss.syntaxpane.DefaultSyntaxKit;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.classhandle.ClassHandleError;
import cuchaz.enigma.events.ClassHandleListener;
import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.config.Config;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.elements.PopupMenuBar;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.events.ThemeChangeListener;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.SelectionHighlightPainter;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Result;

public class EditorPanel {

	private final JPanel ui = new JPanel();
	private final JEditorPane editor = new JEditorPane();
	private final JScrollPane editorScrollPane = new JScrollPane(this.editor);
	private final PopupMenuBar popupMenu;

	// progress UI
	private final JLabel decompilingLabel = new JLabel(I18n.translate("editor.decompiling"), JLabel.CENTER);
	private final JProgressBar decompilingProgressBar = new JProgressBar(0, 100);

	// error display UI
	private final JLabel errorLabel = new JLabel();
	private final JTextArea errorTextArea = new JTextArea();
	private final JScrollPane errorScrollPane = new JScrollPane(this.errorTextArea);
	private final JButton retryButton = new JButton(I18n.translate("general.retry"));

	private DisplayMode mode = DisplayMode.INACTIVE;

	private final GuiController controller;
	private final Gui gui;

	private EntryReference<Entry<?>, Entry<?>> cursorReference;
	private boolean mouseIsPressed = false;
	private boolean shouldNavigateOnClick;

	public Config.LookAndFeel editorLaf;
	private int fontSize = 12;
	private Map<RenamableTokenType, BoxHighlightPainter> boxHighlightPainters;

	private final List<EditorActionListener> listeners = new ArrayList<>();

	private final ThemeChangeListener themeChangeListener;

	private ClassHandle classHandle;
	private DecompiledClassSource source;
	private boolean settingSource;

	public EditorPanel(Gui gui) {
		this.gui = gui;
		this.controller = gui.getController();

		this.editor.setEditable(false);
		this.editor.setSelectionColor(new Color(31, 46, 90));
		this.editor.setCaret(new BrowserCaret());
		this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
		this.editor.addCaretListener(event -> onCaretMove(event.getDot(), this.mouseIsPressed));
		this.editor.setCaretColor(new Color(Config.getInstance().caretColor));
		this.editor.setContentType("text/enigma-sources");
		this.editor.setBackground(new Color(Config.getInstance().editorBackground));
		DefaultSyntaxKit kit = (DefaultSyntaxKit) this.editor.getEditorKit();
		kit.toggleComponent(this.editor, "de.sciss.syntaxpane.components.TokenMarker");

		// init editor popup menu
		this.popupMenu = new PopupMenuBar(this, gui);
		this.editor.setComponentPopupMenu(this.popupMenu);

		this.decompilingLabel.setFont(ScaleUtil.getFont(this.decompilingLabel.getFont().getFontName(), Font.BOLD, 26));
		this.decompilingProgressBar.setIndeterminate(true);
		this.errorTextArea.setEditable(false);
		this.errorTextArea.setFont(ScaleUtil.getFont(Font.MONOSPACED, Font.PLAIN, 10));

		this.boxHighlightPainters = Themes.getBoxHighlightPainters();

		this.editor.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				EditorPanel.this.mouseIsPressed = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				switch (e.getButton()) {
					case MouseEvent.BUTTON3: // Right click
						EditorPanel.this.editor.setCaretPosition(EditorPanel.this.editor.viewToModel(e.getPoint()));
						break;

					case 4: // Back navigation
						gui.getController().openPreviousReference();
						break;

					case 5: // Forward navigation
						gui.getController().openNextReference();
						break;
				}
				EditorPanel.this.mouseIsPressed = false;
			}
		});
		this.editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.isControlDown()) {
					EditorPanel.this.shouldNavigateOnClick = false;
					switch (event.getKeyCode()) {
						case KeyEvent.VK_I:
							EditorPanel.this.popupMenu.showInheritanceMenu.doClick();
							break;

						case KeyEvent.VK_M:
							EditorPanel.this.popupMenu.showImplementationsMenu.doClick();
							break;

						case KeyEvent.VK_N:
							EditorPanel.this.popupMenu.openEntryMenu.doClick();
							break;

						case KeyEvent.VK_P:
							EditorPanel.this.popupMenu.openPreviousMenu.doClick();
							break;

						case KeyEvent.VK_E:
							EditorPanel.this.popupMenu.openNextMenu.doClick();
							break;

						case KeyEvent.VK_C:
							if (event.isShiftDown()) {
								EditorPanel.this.popupMenu.showCallsSpecificMenu.doClick();
							} else {
								EditorPanel.this.popupMenu.showCallsMenu.doClick();
							}
							break;

						case KeyEvent.VK_O:
							EditorPanel.this.popupMenu.toggleMappingMenu.doClick();
							break;

						case KeyEvent.VK_R:
							EditorPanel.this.popupMenu.renameMenu.doClick();
							break;

						case KeyEvent.VK_D:
							EditorPanel.this.popupMenu.editJavadocMenu.doClick();
							break;

						case KeyEvent.VK_F5:
							if (EditorPanel.this.classHandle != null) {
								EditorPanel.this.classHandle.invalidateMapped();
							}
							break;

						case KeyEvent.VK_F:
							// prevent navigating on click when quick find activated
							break;

						case KeyEvent.VK_ADD:
						case KeyEvent.VK_EQUALS:
						case KeyEvent.VK_PLUS:
							offsetEditorZoom(2);
							break;
						case KeyEvent.VK_SUBTRACT:
						case KeyEvent.VK_MINUS:
							offsetEditorZoom(-2);
							break;

						default:
							EditorPanel.this.shouldNavigateOnClick = true; // CTRL
							break;
					}
				}
			}

			@Override
			public void keyTyped(KeyEvent event) {
				if (!EditorPanel.this.popupMenu.renameMenu.isEnabled()) return;

				if (!event.isControlDown() && !event.isAltDown() && Character.isJavaIdentifierPart(event.getKeyChar())) {
					EnigmaProject project = gui.getController().project;
					EntryReference<Entry<?>, Entry<?>> reference = project.getMapper().deobfuscate(EditorPanel.this.cursorReference);
					Entry<?> entry = reference.getNameableEntry();

					String name = String.valueOf(event.getKeyChar());
					if (entry instanceof ClassEntry && ((ClassEntry) entry).getParent() == null) {
						String packageName = ((ClassEntry) entry).getPackageName();
						if (packageName != null) {
							name = packageName + "/" + name;
						}
					}

					gui.startRename(EditorPanel.this, name);
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				EditorPanel.this.shouldNavigateOnClick = event.isControlDown();
			}
		});

		this.retryButton.addActionListener(_e -> redecompileClass());

		this.themeChangeListener = (laf, boxHighlightPainters) -> {
			if ((this.editorLaf == null || this.editorLaf != laf)) {
				this.editor.updateUI();
				this.editor.setBackground(new Color(Config.getInstance().editorBackground));
				if (this.editorLaf != null) {
					this.classHandle.invalidateMapped();
				}

				this.editorLaf = laf;
			}
			this.boxHighlightPainters = boxHighlightPainters;
		};

		this.ui.putClientProperty(EditorPanel.class, this);
	}

	@Nullable
	public static EditorPanel byUi(Component ui) {
		if (ui instanceof JComponent) {
			Object prop = ((JComponent) ui).getClientProperty(EditorPanel.class);
			if (prop instanceof EditorPanel) {
				return (EditorPanel) prop;
			}
		}
		return null;
	}

	public void setClassHandle(ClassHandle handle) {
		ClassEntry old = null;
		if (this.classHandle != null) {
			old = this.classHandle.getRef();
			this.classHandle.close();
		}
		setClassHandle0(old, handle);
	}

	private void setClassHandle0(ClassEntry old, ClassHandle handle) {
		this.setDisplayMode(DisplayMode.IN_PROGRESS);
		setCursorReference(null);

		handle.addListener(new ClassHandleListener() {
			@Override
			public void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
				SwingUtilities.invokeLater(() -> {
					EditorPanel.this.listeners.forEach(l -> l.onTitleChanged(EditorPanel.this, getFileName()));
				});
			}

			@Override
			public void onMappedSourceChanged(ClassHandle h, Result<DecompiledClassSource, ClassHandleError> res) {
				handleDecompilerResult(res);
			}

			@Override
			public void onInvalidate(ClassHandle h, InvalidationType t) {
				SwingUtilities.invokeLater(() -> {
					if (t == InvalidationType.FULL) {
						EditorPanel.this.setDisplayMode(DisplayMode.IN_PROGRESS);
					}
				});
			}

			@Override
			public void onDeleted(ClassHandle h) {
				SwingUtilities.invokeLater(() -> EditorPanel.this.gui.closeEditor(EditorPanel.this));
			}
		});

		handle.getSource().thenAcceptAsync(this::handleDecompilerResult, SwingUtilities::invokeLater);

		this.classHandle = handle;
		this.listeners.forEach(l -> l.onClassHandleChanged(this, old, handle));
	}

	public void setup() {
		Themes.addListener(this.themeChangeListener);
	}

	public void destroy() {
		Themes.removeListener(this.themeChangeListener);
		this.classHandle.close();
	}

	private void redecompileClass() {
		if (this.classHandle != null) {
			this.classHandle.invalidate();
		}
	}

	private void handleDecompilerResult(Result<DecompiledClassSource, ClassHandleError> res) {
		SwingUtilities.invokeLater(() -> {
			if (res.isOk()) {
				this.setSource(res.unwrap());
			} else {
				this.displayError(res.unwrapErr());
			}
		});
	}

	public void displayError(ClassHandleError t) {
		this.setDisplayMode(DisplayMode.ERRORED);
		String str;
		switch(t.type) {
			case DECOMPILE:
				str = "editor.decompile_error";
				break;
			case REMAP:
				str = "editor.remap_error";
				break;
			default:
				throw new IllegalStateException("unreachable");
		}
		this.errorLabel.setText(I18n.translate(str));
		this.errorTextArea.setText(t.getStackTrace());
		this.errorTextArea.setCaretPosition(0);
	}

	public void setDisplayMode(DisplayMode mode) {
		if (this.mode == mode) return;
		this.ui.removeAll();
		switch (mode) {
			case INACTIVE:
				break;
			case IN_PROGRESS: {
				// make progress bar start from the left every time
				this.decompilingProgressBar.setIndeterminate(false);
				this.decompilingProgressBar.setIndeterminate(true);

				this.ui.setLayout(new GridBagLayout());
				GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(ScaleUtil.scale(2));
				this.ui.add(this.decompilingLabel, cb.pos(0, 0).anchor(GridBagConstraints.SOUTH).build());
				this.ui.add(this.decompilingProgressBar, cb.pos(0, 1).anchor(GridBagConstraints.NORTH).build());
				break;
			}
			case SUCCESS: {
				this.ui.setLayout(new GridLayout(1, 1, 0, 0));
				this.ui.add(this.editorScrollPane);
				break;
			}
			case ERRORED: {
				this.ui.setLayout(new GridBagLayout());
				GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(ScaleUtil.scale(2)).weight(1.0, 0.0).anchor(GridBagConstraints.WEST);
				this.ui.add(this.errorLabel, cb.pos(0, 0).build());
				this.ui.add(new JSeparator(JSeparator.HORIZONTAL), cb.pos(0, 1).fill(GridBagConstraints.HORIZONTAL).build());
				this.ui.add(this.errorScrollPane, cb.pos(0, 2).weight(1.0, 1.0).fill(GridBagConstraints.BOTH).build());
				this.ui.add(this.retryButton, cb.pos(0, 3).weight(0.0, 0.0).anchor(GridBagConstraints.EAST).build());
				break;
			}
		}
		this.ui.validate();
		this.ui.repaint();
		this.mode = mode;
	}

	public void offsetEditorZoom(int zoomAmount) {
		int newResult = this.fontSize + zoomAmount;
		if (newResult > 8 && newResult < 72) {
			this.fontSize = newResult;
			this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
		}
	}

	public void resetEditorZoom() {
		this.fontSize = 12;
		this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
	}

	public void onCaretMove(int pos, boolean fromClick) {
		if (this.controller.project == null) return;

		EntryRemapper mapper = this.controller.project.getMapper();
		Token token = getToken(pos);

		if (this.settingSource) {
			EntryReference<Entry<?>, Entry<?>> ref = getCursorReference();
			EntryReference<Entry<?>, Entry<?>> refAtCursor = getReference(token);
			if (this.editor.getDocument().getLength() != 0 && ref != null && !ref.equals(refAtCursor)) {
				showReference0(ref);
			}
			return;
		} else {
			setCursorReference(getReference(token));
		}

		Entry<?> referenceEntry = this.cursorReference != null ? this.cursorReference.entry : null;

		if (referenceEntry != null && this.shouldNavigateOnClick && fromClick) {
			this.shouldNavigateOnClick = false;
			Entry<?> navigationEntry = referenceEntry;
			if (this.cursorReference.context == null) {
				EntryResolver resolver = mapper.getObfResolver();
				navigationEntry = resolver.resolveFirstEntry(referenceEntry, ResolutionStrategy.RESOLVE_ROOT);
			}
			this.controller.navigateTo(navigationEntry);
		}
	}

	private void setCursorReference(EntryReference<Entry<?>, Entry<?>> ref) {
		this.cursorReference = ref;

		Entry<?> referenceEntry = ref == null ? null : ref.entry;

		boolean isClassEntry = referenceEntry instanceof ClassEntry;
		boolean isFieldEntry = referenceEntry instanceof FieldEntry;
		boolean isMethodEntry = referenceEntry instanceof MethodEntry && !((MethodEntry) referenceEntry).isConstructor();
		boolean isConstructorEntry = referenceEntry instanceof MethodEntry && ((MethodEntry) referenceEntry).isConstructor();
		boolean isRenamable = ref != null && this.controller.project.isRenamable(ref);

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

		if (referenceEntry != null && this.controller.project.getMapper().extendedDeobfuscate(referenceEntry).isDeobfuscated()) {
			this.popupMenu.toggleMappingMenu.setText(I18n.translate("popup_menu.reset_obfuscated"));
		} else {
			this.popupMenu.toggleMappingMenu.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		}

		this.listeners.forEach(l -> l.onCursorReferenceChanged(this, ref));
	}

	public Token getToken(int pos) {
		if (this.source == null) {
			return null;
		}
		return this.source.getIndex().getReferenceToken(pos);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (this.source == null) {
			return null;
		}
		return this.source.getIndex().getReference(token);
	}

	public void setSource(DecompiledClassSource source) {
		this.setDisplayMode(DisplayMode.SUCCESS);
		if (source == null) return;
		try {
			this.settingSource = true;
			this.source = source;
			this.editor.getHighlighter().removeAllHighlights();
			this.editor.setText(source.toString());
			setHighlightedTokens(source.getHighlightedTokens());
		} finally {
			this.settingSource = false;
		}
		showReference0(getCursorReference());
	}

	public void setHighlightedTokens(Map<RenamableTokenType, Collection<Token>> tokens) {
		// remove any old highlighters
		this.editor.getHighlighter().removeAllHighlights();

		if (this.boxHighlightPainters != null) {
			for (RenamableTokenType type : tokens.keySet()) {
				BoxHighlightPainter painter = this.boxHighlightPainters.get(type);
				if (painter != null) {
					setHighlightedTokens(tokens.get(type), painter);
				}
			}
		}

		this.editor.validate();
		this.editor.repaint();
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

	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		return this.cursorReference;
	}

	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		setCursorReference(reference);
		showReference0(reference);
	}

	/**
	 * Navigates to the reference without modifying history. Assumes the class is loaded.
	 *
	 * @param reference
	 */
	private void showReference0(EntryReference<Entry<?>, Entry<?>> reference) {
		if (this.source == null) return;
		if (reference == null) return;

		Collection<Token> tokens = this.controller.getTokensForReference(this.source, reference);
		if (tokens.isEmpty()) {
			// DEBUG
			System.err.println(String.format("WARNING: no tokens found for %s in %s", reference, this.classHandle.getRef()));
		} else {
			this.gui.showTokens(this, tokens);
		}
	}

	public void navigateToToken(Token token) {
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null!");
		}
		navigateToToken(token, SelectionHighlightPainter.INSTANCE);
	}

	private void navigateToToken(Token token, HighlightPainter highlightPainter) {
		// set the caret position to the token
		Document document = this.editor.getDocument();
		int clampedPosition = Math.min(Math.max(token.start, 0), document.getLength());

		this.editor.setCaretPosition(clampedPosition);
		this.editor.grabFocus();

		try {
			// make sure the token is visible in the scroll window
			Rectangle start = this.editor.modelToView(token.start);
			Rectangle end = this.editor.modelToView(token.end);
			if (start == null || end == null) {
				return;
			}
			Rectangle show = start.union(end);
			show.grow(start.width * 10, start.height * 6);
			SwingUtilities.invokeLater(() -> this.editor.scrollRectToVisible(show));
		} catch (BadLocationException ex) {
			if (!this.settingSource) {
				throw new RuntimeException(ex);
			} else {
				return;
			}
		}

		// highlight the token momentarily
		Timer timer = new Timer(200, new ActionListener() {
			private int counter = 0;
			private Object highlight = null;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (this.counter % 2 == 0) {
					try {
						this.highlight = EditorPanel.this.editor.getHighlighter().addHighlight(token.start, token.end, highlightPainter);
					} catch (BadLocationException ex) {
						// don't care
					}
				} else if (this.highlight != null) {
					EditorPanel.this.editor.getHighlighter().removeHighlight(this.highlight);
				}

				if (this.counter++ > 6) {
					Timer timer = (Timer) event.getSource();
					timer.stop();
				}
			}
		});
		timer.start();
	}

	public void addListener(EditorActionListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(EditorActionListener listener) {
		this.listeners.remove(listener);
	}

	public JPanel getUi() {
		return this.ui;
	}

	public JEditorPane getEditor() {
		return this.editor;
	}

	public DecompiledClassSource getSource() {
		return this.source;
	}

	public ClassHandle getClassHandle() {
		return this.classHandle;
	}

	public String getFileName() {
		ClassEntry classEntry = this.classHandle.getDeobfRef() != null ? this.classHandle.getDeobfRef() : this.classHandle.getRef();
		return classEntry.getSimpleName();
	}

	private enum DisplayMode {
		INACTIVE,
		IN_PROGRESS,
		SUCCESS,
		ERRORED,
	}

}
