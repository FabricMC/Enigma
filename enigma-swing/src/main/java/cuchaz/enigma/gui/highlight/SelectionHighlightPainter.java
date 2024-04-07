/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.gui.highlight;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.util.ScaleUtil;

public class SelectionHighlightPainter implements Highlighter.HighlightPainter {
	public static final SelectionHighlightPainter INSTANCE = new SelectionHighlightPainter();

	@Override
	public void paint(Graphics g, int start, int end, Shape shape, JTextComponent text) {
		// draw a thick border
		Graphics2D g2d = (Graphics2D) g;
		Rectangle bounds = BoxHighlightPainter.getBounds(text, start, end);
		g2d.setColor(UiConfig.getSelectionHighlightColor());
		g2d.setStroke(new BasicStroke(ScaleUtil.scale(2.0f)));

		int arcSize = ScaleUtil.scale(4);
		g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arcSize, arcSize);
	}
}
