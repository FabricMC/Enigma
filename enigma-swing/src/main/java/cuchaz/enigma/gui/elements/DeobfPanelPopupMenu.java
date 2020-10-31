package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

public class DeobfPanelPopupMenu {

    private final JPopupMenu ui;
    private final JMenuItem renamePackage;
    private final JMenuItem renameClass;

    public DeobfPanelPopupMenu(Gui gui) {
        this.ui = new JPopupMenu();

        ClassSelector deobfClasses = gui.getDeobfPanel().deobfClasses;

        this.renamePackage = new JMenuItem();
        this.renamePackage.addActionListener(a -> {
            TreePath path;

            if (deobfClasses.getSelectedClass() != null) {
                // Rename parent package if selected path is a class
                path = deobfClasses.getSelectionPath().getParentPath();
            } else {
                // Rename selected path if it's already a package
                path = deobfClasses.getSelectionPath();
            }

            deobfClasses.getUI().startEditingAtPath(deobfClasses, path);
        });
        this.ui.add(this.renamePackage);

        this.renameClass = new JMenuItem();
        this.renameClass.addActionListener(a -> deobfClasses.getUI().startEditingAtPath(deobfClasses, deobfClasses.getSelectionPath()));
        this.ui.add(this.renameClass);

        this.retranslateUi();
    }

    public void show(ClassSelector deobfClasses, int x, int y) {
        // Only enable rename class if selected path is a class
        this.renameClass.setEnabled(deobfClasses.getSelectedClass() != null);

        this.ui.show(deobfClasses, x, y);
    }

    public void retranslateUi() {
        this.renamePackage.setText(I18n.translate("popup_menu.deobf_panel.rename_package"));
        this.renameClass.setText(I18n.translate("popup_menu.deobf_panel.rename_class"));
    }
}
