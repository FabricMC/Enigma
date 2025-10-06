package cuchaz.enigma.gui.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.github.weisj.jsvg.SVGDocument;

import cuchaz.enigma.api.EnigmaIcon;

public class EnigmaIconImpl implements EnigmaIcon, Icon {
	private final SVGDocument svg;

	public EnigmaIconImpl(SVGDocument svg) {
		this.svg = svg;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		svg.render(c instanceof JComponent jc ? jc : null, g2);
	}

	@Override
	public int getIconWidth() {
		return Math.round(svg.size().width);
	}

	@Override
	public int getIconHeight() {
		return Math.round(svg.size().height);
	}
}
