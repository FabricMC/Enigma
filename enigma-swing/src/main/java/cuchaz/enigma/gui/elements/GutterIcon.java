package cuchaz.enigma.gui.elements;

import java.awt.Cursor;

import javax.swing.JButton;

import cuchaz.enigma.api.service.GuiService;
import cuchaz.enigma.gui.util.EnigmaIconImpl;

public class GutterIcon extends JButton implements GuiService.GutterMarkerBuilder {
	private Runnable clickAction = () -> { };

	public GutterIcon(EnigmaIconImpl icon) {
		super(icon);
		setContentAreaFilled(false);
		setCursor(Cursor.getDefaultCursor());
		addActionListener(e -> clickAction.run());
	}

	@Override
	public GuiService.GutterMarkerBuilder setClickAction(Runnable action) {
		this.clickAction = action;
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return this;
	}

	@Override
	public GuiService.GutterMarkerBuilder setTooltip(String tooltip) {
		setToolTipText(tooltip);
		return this;
	}
}
