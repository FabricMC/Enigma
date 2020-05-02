package cuchaz.enigma.gui.panels;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.config.Config;
import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.DecompiledClassSource;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.elements.PopupMenuBar;
import cuchaz.enigma.gui.events.ClassHandleListener;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.events.ThemeChangeListener;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import cuchaz.enigma.gui.util.ClassHandle;
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

	private final JEditorPane ui;
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

	public PanelEditor(Gui gui, ClassHandle handle) {
		this.gui = gui;
		this.controller = gui.getController();

		this.ui = new JEditorPane();
		this.ui.setEditable(false);
		this.ui.setSelectionColor(new Color(31, 46, 90));
		this.ui.setCaret(new BrowserCaret());
		this.ui.setFont(ScaleUtil.getFont(this.ui.getFont().getFontName(), Font.PLAIN, this.fontSize));
		this.ui.addCaretListener(event -> onCaretMove(event.getDot(), mouseIsPressed));
		this.ui.setCaretColor(new Color(Config.getInstance().caretColor));
		this.ui.setContentType("text/enigma-sources");
		this.ui.setBackground(new Color(Config.getInstance().editorBackground));
		DefaultSyntaxKit kit = (DefaultSyntaxKit) this.ui.getEditorKit();
		kit.toggleComponent(this.ui, "de.sciss.syntaxpane.components.TokenMarker");

		// init editor popup menu
		this.popupMenu = new PopupMenuBar(this, gui);
		this.ui.setComponentPopupMenu(this.popupMenu);

		this.boxHighlightPainters = Themes.getBoxHighlightPainters();

		this.ui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				mouseIsPressed = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				switch (e.getButton()) {
					case MouseEvent.BUTTON3: // Right click
						ui.setCaretPosition(ui.viewToModel(e.getPoint()));
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
		this.ui.addKeyListener(new KeyAdapter() {
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

					popupMenu.renameMenu.doClick();
					gui.renameTextField.setText(name);
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				shouldNavigateOnClick = event.isControlDown();
			}
		});

		themeChangeListener = (laf, boxHighlightPainters) -> {
			if ((editorLaf == null || editorLaf != laf)) {
				this.ui.updateUI();
				this.ui.setBackground(new Color(Config.getInstance().editorBackground));
				if (editorLaf != null) {
					gui.getController().getClassHandleProvider().invalidateMapped(classHandle.getRef());
				}

				editorLaf = laf;
			}
			this.boxHighlightPainters = boxHighlightPainters;
		};

		setClassHandle0(handle);
	}

	public void setClassHandle(ClassHandle handle) {
		this.classHandle.close();
		setClassHandle0(handle);
	}

	private void setClassHandle0(ClassHandle handle) {
		setCursorReference(null);

		handle.addListener(new ClassHandleListener() {
			@Override
			public void onDeobfRefChanged(ClassHandle h) {
				listeners.forEach(l -> l.onTitleChanged(PanelEditor.this, getFileName()));
			}

			@Override
			public void onMappedSourceChanged(ClassHandle h, DecompiledClassSource s) {
				setSource(s);
				showReference0(cursorReference);
			}

			@Override
			public void onDeleted(ClassHandle h) {

			}
		});

		handle.getSource().thenAccept(s -> setSource(s));

		this.classHandle = handle;
		listeners.forEach(l -> l.onClassHandleChanged(this, handle));
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
			this.ui.setFont(ScaleUtil.getFont(this.ui.getFont().getFontName(), Font.PLAIN, this.fontSize));
		}
	}

	public void resetEditorZoom() {
		this.fontSize = 12;
		this.ui.setFont(ScaleUtil.getFont(this.ui.getFont().getFontName(), Font.PLAIN, this.fontSize));
	}

	public void onCaretMove(int pos, boolean fromClick) {
		if (controller.project == null) return;

		EntryRemapper mapper = controller.project.getMapper();
		Token token = getToken(pos);

		setCursorReference(getReference(token));

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
		ui.getHighlighter().removeAllHighlights();
		ui.setText(source);
	}

	public void setSource(DecompiledClassSource source) {
		this.source = source;
		ui.setText(source.toString());
		setHighlightedTokens(source.getHighlightedTokens());
	}

	public void setHighlightedTokens(Map<TokenHighlightType, Collection<Token>> tokens) {
		// remove any old highlighters
		ui.getHighlighter().removeAllHighlights();

		if (boxHighlightPainters != null) {
			for (TokenHighlightType type : tokens.keySet()) {
				BoxHighlightPainter painter = boxHighlightPainters.get(type);
				if (painter != null) {
					setHighlightedTokens(tokens.get(type), painter);
				}
			}
		}

		ui.validate();
		ui.repaint();
	}

	private void setHighlightedTokens(Iterable<Token> tokens, Highlighter.HighlightPainter painter) {
		for (Token token : tokens) {
			try {
				ui.getHighlighter().addHighlight(token.start, token.end, painter);
			} catch (BadLocationException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}

	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		return cursorReference;
	}

	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		cursorReference = reference;
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

	public void addListener(EditorActionListener listener) {
		listeners.add(listener);
	}

	public void removeListener(EditorActionListener listener) {
		listeners.remove(listener);
	}

	public JEditorPane getUi() {
		return ui;
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
