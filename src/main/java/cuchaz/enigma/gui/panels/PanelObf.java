package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;

public class PanelObf extends JPanel {

    private final Gui gui;
    public final ClassSelector obfClasses;

    public PanelObf(Gui gui) {
        this.gui = gui;

        this.obfClasses = new ClassSelector(ClassSelector.ObfuscatedClassEntryComparator);
        this.obfClasses.setListener(gui::navigateTo);

        this.setLayout(new BorderLayout());
        this.add(new JLabel("Obfuscated Classes"), BorderLayout.NORTH);
        this.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);
    }
}
