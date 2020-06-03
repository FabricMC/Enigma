package cuchaz.enigma.gui.panels;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.gui.util.ScaleUtil;

import javax.swing.*;
import java.awt.*;

public class PanelIdentifier extends JPanel {

	private final Gui gui;

	public PanelIdentifier(Gui gui) {
		this.gui = gui;

		this.setLayout(new GridLayout(4, 1, 0, 0));
		this.setPreferredSize(ScaleUtil.getDimension(0, 100));
		this.setBorder(BorderFactory.createTitledBorder(I18n.translate("info_panel.identifier")));
	}

	public void clearReference() {
		this.removeAll();
		JLabel label = new JLabel(I18n.translate("info_panel.identifier.none"));
		GuiUtil.unboldLabel(label);
		label.setHorizontalAlignment(JLabel.CENTER);
		this.add(label);

		gui.redraw();
	}
}
