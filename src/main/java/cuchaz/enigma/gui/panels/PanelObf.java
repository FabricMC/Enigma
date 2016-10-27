package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.util.Comparator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.mapping.ClassEntry;

public class PanelObf extends JPanel {

    private final Gui gui;
    public final ClassSelector obfClasses;

    public PanelObf(Gui gui) {
        this.gui = gui;

        Comparator<ClassEntry> obfClassComparator = (a, b) -> {
            String aname = a.getName();
            String bname = b.getName();
            if (aname.length() != bname.length()) {
                return aname.length() - bname.length();
            }
            return aname.compareTo(bname);
        };

        this.obfClasses = new ClassSelector(gui, obfClassComparator, false);
        this.obfClasses.setSelectionListener(gui::navigateTo);
        this.obfClasses.setRenameSelectionListener(gui::onPanelRename);

        this.setLayout(new BorderLayout());
        this.add(new JLabel("Obfuscated Classes"), BorderLayout.NORTH);
        this.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);
    }
}
