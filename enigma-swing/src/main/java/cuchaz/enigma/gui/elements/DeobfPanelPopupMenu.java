package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import java.awt.*;

public class DeobfPanelPopupMenu {

    private final JPopupMenu ui;
    private final JMenuItem rename;

    public DeobfPanelPopupMenu(Gui gui) {
        this.ui = new JPopupMenu();

        ClassSelector deobfClasses = gui.getDeobfPanel().deobfClasses;

        this.rename = new JMenuItem();
        this.rename.addActionListener(a -> deobfClasses.getUI().startEditingAtPath(deobfClasses, deobfClasses.getSelectionPath()));
        this.ui.add(this.rename);

        this.retranslateUi();
    }

    public void show(Component invoker, int x, int y) {
        this.ui.show(invoker, x, y);
    }

    public void retranslateUi() {
        this.rename.setText(I18n.translate("popup_menu.deobf_panel.rename"));
    }
}
