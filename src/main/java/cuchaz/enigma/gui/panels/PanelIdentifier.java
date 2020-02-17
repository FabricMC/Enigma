package cuchaz.enigma.gui.panels;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.LangUtils;
import cuchaz.enigma.utils.Utils;

import javax.swing.*;
import java.awt.*;

public class PanelIdentifier extends JPanel {

	private final Gui gui;

	public PanelIdentifier(Gui gui) {
		this.gui = gui;

		this.setLayout(new GridLayout(4, 1, 0, 0));
		this.setPreferredSize(new Dimension(0, 100));
		this.setBorder(BorderFactory.createTitledBorder(LangUtils.translate("info_panel.identifier")));
	}

	public void clearReference() {
		this.removeAll();
		JLabel label = new JLabel(LangUtils.translate("info_panel.identifier.none"));
		Utils.unboldLabel(label);
		label.setHorizontalAlignment(JLabel.CENTER);
		this.add(label);

		gui.redraw();
	}
}
