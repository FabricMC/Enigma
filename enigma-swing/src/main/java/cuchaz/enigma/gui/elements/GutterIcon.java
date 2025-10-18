package cuchaz.enigma.gui.elements;

import java.awt.Cursor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;

import cuchaz.enigma.api.service.GuiService;
import cuchaz.enigma.gui.util.EnigmaIconImpl;
import cuchaz.enigma.gui.util.ScaleUtil;

public class GutterIcon extends JButton implements GuiService.GutterMarkerBuilder {
	private Runnable clickAction = () -> { };

	public GutterIcon(EnigmaIconImpl icon) {
		super(icon.icon());
		setContentAreaFilled(false);
		setCursor(Cursor.getDefaultCursor());
		addActionListener(e -> clickAction.run());

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				setIcon(icon.icon().derive(ScaleUtil.invert(getWidth()), ScaleUtil.invert(getHeight())));
			}
		});
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
