package cuchaz.enigma.gui.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;

import javax.annotation.Nullable;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.events.ClassHandleListener;
import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.DecompiledClassSource;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.config.Config;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.elements.PopupMenuBar;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.events.ThemeChangeListener;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.SelectionHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class PanelEditor {

	private final JScrollPane ui;
	private final JEditorPane editor;
	private final PopupMenuBar popupMenu;

	private final GuiController controller;
	private final Gui gui;

	private EntryReference<Entry<?>, Entry<?>> cursorReference;
	private boolean mouseIsPressed = false;
	private boolean shouldNavigateOnClick;

	public Config.LookAndFeel editorLaf;
	private int fontSize = 12;
	private Map<TokenHighlightType, BoxHighlightPainter> boxHighlightPainters;

	private final List<EditorActionListener> listeners = new ArrayList<>();

	private final ThemeChangeListener themeChangeListener;

	private ClassHandle classHandle;
	private DecompiledClassSource source;
	private boolean settingSource;

	public PanelEditor(Gui gui, ClassHandle handle) {
		this.gui = gui;
		this.controller = gui.getController();

		this.editor = new JEditorPane();
		this.ui = new JScrollPane(editor);
		this.editor.setEditable(false);
		this.editor.setSelectionColor(new Color(31, 46, 90));
		this.editor.setCaret(new BrowserCaret());
		this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
		this.editor.addCaretListener(event -> onCaretMove(event.getDot(), mouseIsPressed));
		this.editor.setCaretColor(new Color(Config.getInstance().caretColor));
		this.editor.setContentType("text/enigma-sources");
		this.editor.setBackground(new Color(Config.getInstance().editorBackground));
		DefaultSyntaxKit kit = (DefaultSyntaxKit) this.editor.getEditorKit();
		kit.toggleComponent(this.editor, "de.sciss.syntaxpane.components.TokenMarker");

		// init editor popup menu
		this.popupMenu = new PopupMenuBar(this, gui);
		this.editor.setComponentPopupMenu(this.popupMenu);

		this.boxHighlightPainters = Themes.getBoxHighlightPainters();

		this.editor.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				mouseIsPressed = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				switch (e.getButton()) {
					case MouseEvent.BUTTON3: // Right click
						editor.setCaretPosition(editor.viewToModel(e.getPoint()));
						break;

					case 4: // Back navigation
						gui.getController().openPreviousReference();
						break;

					case 5: // Forward navigation
						gui.getController().openNextReference();
						break;
				}
				mouseIsPressed = false;
			}
		});
		this.editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.isControlDown()) {
					shouldNavigateOnClick = false;
					switch (event.getKeyCode()) {
						case KeyEvent.VK_I:
							popupMenu.showInheritanceMenu.doClick();
							break;

						case KeyEvent.VK_M:
							popupMenu.showImplementationsMenu.doClick();
							break;

						case KeyEvent.VK_N:
							popupMenu.openEntryMenu.doClick();
							break;

						case KeyEvent.VK_P:
							popupMenu.openPreviousMenu.doClick();
							break;

						case KeyEvent.VK_E:
							popupMenu.openNextMenu.doClick();
							break;

						case KeyEvent.VK_C:
							if (event.isShiftDown()) {
								popupMenu.showCallsSpecificMenu.doClick();
							} else {
								popupMenu.showCallsMenu.doClick();
							}
							break;

						case KeyEvent.VK_O:
							popupMenu.toggleMappingMenu.doClick();
							break;

						case KeyEvent.VK_R:
							popupMenu.renameMenu.doClick();
							break;

						case KeyEvent.VK_D:
							popupMenu.editJavadocMenu.doClick();
							break;

						case KeyEvent.VK_F5:
							gui.getController().getClassHandleProvider().invalidateMapped(classHandle.getRef());
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
							shouldNavigateOnClick = true; // CTRL
							break;
					}
				}
			}

			@Override
			public void keyTyped(KeyEvent event) {
				if (!popupMenu.renameMenu.isEnabled()) return;

				if (!event.isControlDown() && !event.isAltDown() && Character.isJavaIdentifierPart(event.getKeyChar())) {
					EnigmaProject project = gui.getController().project;
					EntryReference<Entry<?>, Entry<?>> reference = project.getMapper().deobfuscate(cursorReference);
					Entry<?> entry = reference.getNameableEntry();

					String name = String.valueOf(event.getKeyChar());
					if (entry instanceof ClassEntry && ((ClassEntry) entry).getParent() == null) {
						String packageName = ((ClassEntry) entry).getPackageName();
						if (packageName != null) {
							name = packageName + "/" + name;
						}
					}

					gui.startRename(PanelEditor.this, name);
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				shouldNavigateOnClick = event.isControlDown();
			}
		});

		themeChangeListener = (laf, boxHighlightPainters) -> {
			if ((editorLaf == null || editorLaf != laf)) {
				this.editor.updateUI();
				this.editor.setBackground(new Color(Config.getInstance().editorBackground));
				if (editorLaf != null) {
					gui.getController().getClassHandleProvider().invalidateMapped(classHandle.getRef());
				}

				editorLaf = laf;
			}
			this.boxHighlightPainters = boxHighlightPainters;
		};

		setClassHandle0(null, handle);

		ui.putClientProperty(PanelEditor.class, this);
	}

	@Nullable
	public static PanelEditor byUi(Component ui) {
		if (ui instanceof JComponent) {
			Object prop = ((JComponent) ui).getClientProperty(PanelEditor.class);
			if (prop instanceof PanelEditor) {
				return (PanelEditor) prop;
			}
		}
		return null;
	}

	public void setClassHandle(ClassHandle handle) {
		ClassEntry old = this.classHandle.getRef();
		this.classHandle.close();
		setClassHandle0(old, handle);
	}

	private void setClassHandle0(ClassEntry old, ClassHandle handle) {
		setCursorReference(null);

		handle.addListener(new ClassHandleListener() {
			@Override
			public void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
				SwingUtilities.invokeLater(() -> {
					listeners.forEach(l -> l.onTitleChanged(PanelEditor.this, getFileName()));
				});
			}

			@Override
			public void onMappedSourceChanged(ClassHandle h, DecompiledClassSource s) {
				SwingUtilities.invokeLater(() -> setSource(s));
			}

			@Override
			public void onDeleted(ClassHandle h) {
			}
		});

		handle.getSource().thenAcceptAsync(s -> setSource(s), SwingUtilities::invokeLater);

		this.classHandle = handle;
		listeners.forEach(l -> l.onClassHandleChanged(this, old, handle));
	}

	public void setup() {
		Themes.addListener(themeChangeListener);
	}

	public void destroy() {
		Themes.removeListener(themeChangeListener);
		classHandle.close();
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
		if (controller.project == null) return;

		EntryRemapper mapper = controller.project.getMapper();
		Token token = getToken(pos);

		if (settingSource) {
			EntryReference<Entry<?>, Entry<?>> ref = getCursorReference();
			EntryReference<Entry<?>, Entry<?>> refAtCursor = getReference(token);
			if (editor.getDocument().getLength() != 0 && !Objects.equals(refAtCursor, ref)) {
				showReference0(ref);
			}
			return;
		} else {
			setCursorReference(getReference(token));
		}

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

		if (referenceEntry != null && referenceEntry.equals(controller.project.getMapper().deobfuscate(referenceEntry))) {
			this.popupMenu.toggleMappingMenu.setText(I18n.translate("popup_menu.reset_obfuscated"));
		} else {
			this.popupMenu.toggleMappingMenu.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		}

		listeners.forEach(l -> l.onCursorReferenceChanged(this, ref));
	}

	public Token getToken(int pos) {
		if (source == null) {
			return null;
		}
		return source.getIndex().getReferenceToken(pos);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (source == null) {
			return null;
		}
		return source.getIndex().getReference(token);
	}

	public void setEditorText(String source) {
		editor.getHighlighter().removeAllHighlights();
		editor.setText(source);
	}

	public void setSource(DecompiledClassSource source) {
		try {
			settingSource = true;
			this.source = source;
			editor.setText(source.toString());
			setHighlightedTokens(source.getHighlightedTokens());
		} finally {
			settingSource = false;
		}
		showReference0(getCursorReference());
	}

	public void setHighlightedTokens(Map<TokenHighlightType, Collection<Token>> tokens) {
		// remove any old highlighters
		editor.getHighlighter().removeAllHighlights();

		if (boxHighlightPainters != null) {
			for (TokenHighlightType type : tokens.keySet()) {
				BoxHighlightPainter painter = boxHighlightPainters.get(type);
				if (painter != null) {
					setHighlightedTokens(tokens.get(type), painter);
				}
			}
		}

		editor.validate();
		editor.repaint();
	}

	private void setHighlightedTokens(Iterable<Token> tokens, Highlighter.HighlightPainter painter) {
		for (Token token : tokens) {
			try {
				editor.getHighlighter().addHighlight(token.start, token.end, painter);
			} catch (BadLocationException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}

	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		return cursorReference;
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
		if (source == null) return;

		Collection<Token> tokens = controller.getTokensForReference(source, reference);
		if (tokens.isEmpty()) {
			// DEBUG
			System.err.println(String.format("WARNING: no tokens found for %s in %s", reference, classHandle.getRef()));
		} else {
			gui.showTokens(this, tokens);
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
		Document document = editor.getDocument();
		int clampedPosition = Math.min(Math.max(token.start, 0), document.getLength());

		editor.setCaretPosition(clampedPosition);
		editor.grabFocus();

		try {
			// make sure the token is visible in the scroll window
			Rectangle start = editor.modelToView(token.start);
			Rectangle end = editor.modelToView(token.end);
			Rectangle show = start.union(end);
			show.grow(start.width * 10, start.height * 6);
			SwingUtilities.invokeLater(() -> editor.scrollRectToVisible(show));
		} catch (BadLocationException ex) {
			throw new Error(ex);
		}

		// highlight the token momentarily
		Timer timer = new Timer(200, new ActionListener() {
			private int counter = 0;
			private Object highlight = null;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (counter % 2 == 0) {
					try {
						highlight = editor.getHighlighter().addHighlight(token.start, token.end, highlightPainter);
					} catch (BadLocationException ex) {
						// don't care
					}
				} else if (highlight != null) {
					editor.getHighlighter().removeHighlight(highlight);
				}

				if (counter++ > 6) {
					Timer timer = (Timer) event.getSource();
					timer.stop();
				}
			}
		});
		timer.start();
	}

	public void addListener(EditorActionListener listener) {
		listeners.add(listener);
	}

	public void removeListener(EditorActionListener listener) {
		listeners.remove(listener);
	}

	public JScrollPane getUi() {
		return ui;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public DecompiledClassSource getSource() {
		return source;
	}

	public ClassHandle getClassHandle() {
		return classHandle;
	}

	public String getFileName() {
		ClassEntry classEntry = classHandle.getDeobfRef() != null ? classHandle.getDeobfRef() : classHandle.getRef();
		return classEntry.getSimpleName();
	}

}
