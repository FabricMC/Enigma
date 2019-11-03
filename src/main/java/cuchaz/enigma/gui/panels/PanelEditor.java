package cuchaz.enigma.gui.panels;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.config.Config;
import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PanelEditor extends JEditorPane {
	private boolean mouseIsPressed = false;

	public PanelEditor(Gui gui) {
		this.setEditable(false);
		this.setSelectionColor(new Color(31, 46, 90));
		this.setCaret(new BrowserCaret());
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
							gui.popupMenu.showCallsMenu.doClick();
							break;

						case KeyEvent.VK_O:
							gui.popupMenu.toggleMappingMenu.doClick();
							break;

						case KeyEvent.VK_R:
							gui.popupMenu.renameMenu.doClick();
							break;

						case KeyEvent.VK_F5:
							gui.getController().refreshCurrentClass();
							break;

						case KeyEvent.VK_F:
							// prevent navigating on click when quick find activated
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

	@Override
	public Color getCaretColor() {
		return new Color(Config.getInstance().caretColor);
	}
}
