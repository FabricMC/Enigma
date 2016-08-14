package cuchaz.enigma.gui.panels;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JEditorPane;

import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.Gui;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class PanelEditor extends JEditorPane {
    private final Gui gui;

    public PanelEditor(Gui gui) {
        this.gui = gui;

        this.setEditable(false);
        this.setCaret(new BrowserCaret());
        this.setContentType("text/java");
        this.addCaretListener(event -> gui.onCaretMove(event.getDot()));
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_R:
                        gui.popupMenu.renameMenu.doClick();
                        break;

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

                    case KeyEvent.VK_C:
                        gui.popupMenu.showCallsMenu.doClick();
                        break;

                    case KeyEvent.VK_T:
                        gui.popupMenu.toggleMappingMenu.doClick();
                        break;
                    case KeyEvent.VK_F5:
                        gui.getController().refreshCurrentClass();
                    default:
                        break;
                }
            }
        });

        DefaultSyntaxKit kit = (DefaultSyntaxKit) this.getEditorKit();
        kit.toggleComponent(this, "de.sciss.syntaxpane.components.TokenMarker");
    }
}
