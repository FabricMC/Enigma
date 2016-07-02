package cuchaz.enigma.gui.panels;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiTricks;

public class PanelIdentifier extends JPanel {

    private final Gui gui;

    public PanelIdentifier(Gui gui) {
        this.gui = gui;

        this.setLayout(new GridLayout(4, 1, 0, 0));
        this.setPreferredSize(new Dimension(0, 100));
        this.setBorder(BorderFactory.createTitledBorder("Identifier Info"));
    }

    public void clearReference() {
        this.removeAll();
        JLabel label = new JLabel("No identifier selected");
        GuiTricks.unboldLabel(label);
        label.setHorizontalAlignment(JLabel.CENTER);
        this.add(label);

        gui.redraw();
    }
}
