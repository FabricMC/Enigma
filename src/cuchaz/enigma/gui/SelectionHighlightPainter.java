/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

public class SelectionHighlightPainter implements Highlighter.HighlightPainter {
	
	@Override
	public void paint(Graphics g, int start, int end, Shape shape, JTextComponent text) {
		// draw a thick border
		Graphics2D g2d = (Graphics2D)g;
		Rectangle bounds = BoxHighlightPainter.getBounds(text, start, end);
		g2d.setColor(Color.black);
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 4, 4);
	}
}
