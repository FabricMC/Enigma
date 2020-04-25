/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui.highlight;

import cuchaz.enigma.gui.config.GuiConfig;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class BoxHighlightPainter implements Highlighter.HighlightPainter {
	private Color fillColor;
	private Color borderColor;

	protected BoxHighlightPainter(Color fillColor, Color borderColor) {
		this.fillColor = fillColor;
		this.borderColor = borderColor;
	}

	public static BoxHighlightPainter create(GuiConfig.AlphaColorEntry entry, GuiConfig.AlphaColorEntry entryOutline) {
		return new BoxHighlightPainter(entry != null ? entry.get() : null, entryOutline != null ? entryOutline.get() : null);
	}

	public static Rectangle getBounds(JTextComponent text, int start, int end) {
		try {
			// determine the bounds of the text
			Rectangle startRect = text.modelToView(start);
			Rectangle endRect = text.modelToView(end);
			Rectangle bounds = startRect.union(endRect);

			// adjust the box so it looks nice
			bounds.x -= 2;
			bounds.width += 2;
			bounds.y += 1;
			bounds.height -= 2;

			return bounds;
		} catch (BadLocationException ex) {
			// don't care... just return something
			return new Rectangle(0, 0, 0, 0);
		}
	}

	@Override
	public void paint(Graphics g, int start, int end, Shape shape, JTextComponent text) {
		Rectangle bounds = getBounds(text, start, end);

		// fill the area
		if (this.fillColor != null) {
			g.setColor(this.fillColor);
			g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 4, 4);
		}

		// draw a box around the area
		g.setColor(this.borderColor);
		g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 4, 4);
	}

}
