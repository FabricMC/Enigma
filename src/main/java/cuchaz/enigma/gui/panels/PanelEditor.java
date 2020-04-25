package cuchaz.enigma.gui.panels;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.config.GuiConfig;
import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.gui.util.ScaleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PanelEditor extends JEditorPane {
	private boolean mouseIsPressed = false;
	public int fontSize = 12;

	public PanelEditor(Gui gui) {
		this.setEditable(false);
		this.setSelectionColor(new Color(31, 46, 90));
		this.setCaret(new BrowserCaret());
		this.setFont(ScaleUtil.getFont(this.getFont().getFontName(), Font.PLAIN, this.fontSize));
		this.addCaretListener(event -> gui.onCaretMove(event.getDot(), mouseIsPressed));
		final PanelEditor self = this;
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				mouseIsPressed = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				switch (e.getButton()) {
					case MouseEvent.BUTTON3: // Right click
						self.setCaretPosition(self.viewToModel(e.getPoint()));
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
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.isControlDown()) {
					gui.setShouldNavigateOnClick(false);
					switch (event.getKeyCode()) {
						case KeyEvent.VK_I:
							gui.popupMenu.showInheritanceMenu.doClick();
							break;

						case KeyEvent.VK_M:
							gui.popupMenu.showImplementationsMenu.doClick();
							break;

						case KeyEvent.VK_N:
							gui.popupMenu.openEntryMenu.doClick();
							break;

						case KeyEvent.VK_P:
							gui.popupMenu.openPreviousMenu.doClick();
							break;

						case KeyEvent.VK_E:
							gui.popupMenu.openNextMenu.doClick();
							break;

						case KeyEvent.VK_C:
							if (event.isShiftDown()) {
								gui.popupMenu.showCallsSpecificMenu.doClick();
							} else {
								gui.popupMenu.showCallsMenu.doClick();
							}
							break;

						case KeyEvent.VK_O:
							gui.popupMenu.toggleMappingMenu.doClick();
							break;

						case KeyEvent.VK_R:
							gui.popupMenu.renameMenu.doClick();
							break;

						case KeyEvent.VK_D:
							gui.popupMenu.editJavadocMenu.doClick();
							break;

						case KeyEvent.VK_F5:
							gui.getController().refreshCurrentClass();
							break;

						case KeyEvent.VK_F:
							// prevent navigating on click when quick find activated
							break;

						case KeyEvent.VK_ADD:
						case KeyEvent.VK_EQUALS:
						case KeyEvent.VK_PLUS:
							self.offsetEditorZoom(2);
							break;
						case KeyEvent.VK_SUBTRACT:
						case KeyEvent.VK_MINUS:
							self.offsetEditorZoom(-2);
							break;

						default:
							gui.setShouldNavigateOnClick(true); // CTRL
							break;
					}
				}
			}

			@Override
			public void keyTyped(KeyEvent event) {
				if (!gui.popupMenu.renameMenu.isEnabled()) return;

				if (!event.isControlDown() && !event.isAltDown()) {
					EnigmaProject project = gui.getController().project;
					EntryReference<Entry<?>, Entry<?>> reference = project.getMapper().deobfuscate(gui.cursorReference);
					Entry<?> entry = reference.getNameableEntry();

					String name = String.valueOf(event.getKeyChar());
					if (entry instanceof ClassEntry && ((ClassEntry) entry).getParent() == null) {
						String packageName = ((ClassEntry) entry).getPackageName();
						if (packageName != null) {
							name = packageName + "/" + name;
						}
					}

					gui.popupMenu.renameMenu.doClick();
					gui.renameTextField.setText(name);
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				gui.setShouldNavigateOnClick(event.isControlDown());
			}
		});
	}

	public void offsetEditorZoom(int zoomAmount) {
		int newResult = this.fontSize + zoomAmount;
		if (newResult > 8 && newResult < 72) {
			this.fontSize = newResult;
			this.setFont(ScaleUtil.getFont(this.getFont().getFontName(), Font.PLAIN, this.fontSize));
		}
	}

	public void resetEditorZoom() {
		this.fontSize = 12;
		this.setFont(ScaleUtil.getFont(this.getFont().getFontName(), Font.PLAIN, this.fontSize));
	}

	@Override
	public Color getCaretColor() {
		return new Color(GuiConfig.getInstance().caretColor);
	}
}
