package cuchaz.enigma.gui.elements;

import java.awt.Cursor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import cuchaz.enigma.api.service.GuiService;

public class GutterIcon extends JButton implements GuiService.GutterMarkerBuilder {
	private Runnable clickAction = () -> { };

	public GutterIcon(BufferedImage icon) {
		super(new ImageIcon(icon));
		setContentAreaFilled(false);
		setCursor(Cursor.getDefaultCursor());
		addActionListener(e -> clickAction.run());

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				setIcon(new ImageIcon(icon.getScaledInstance(getWidth(), getHeight(), BufferedImage.SCALE_SMOOTH)));
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
