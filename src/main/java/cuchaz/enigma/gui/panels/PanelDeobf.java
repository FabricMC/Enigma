package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;

public class PanelDeobf extends JPanel {

    public final ClassSelector deobfClasses;
    private final Gui gui;

    public PanelDeobf(Gui gui) {
        this.gui = gui;

        this.deobfClasses = new ClassSelector(ClassSelector.DeobfuscatedClassEntryComparator);
        this.deobfClasses.setListener(gui::navigateTo);

        this.setLayout(new BorderLayout());
        this.add(new JLabel("De-obfuscated Classes"), BorderLayout.NORTH);
        this.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);

    }
}
