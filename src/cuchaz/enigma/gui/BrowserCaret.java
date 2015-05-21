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

import java.awt.Graphics;
import java.awt.Shape;

import javax.swing.text.DefaultCaret;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

public class BrowserCaret extends DefaultCaret {
	
	private static final long serialVersionUID = 1158977422507969940L;
	
	private static final Highlighter.HighlightPainter m_selectionPainter = new Highlighter.HighlightPainter() {
		@Override
		public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
			// don't paint anything
		}
	};
	
	@Override
	public boolean isSelectionVisible() {
		return false;
	}
	
	@Override
	public boolean isVisible() {
		return true;
	}
	
	@Override
	public Highlighter.HighlightPainter getSelectionPainter() {
		return m_selectionPainter;
	}
}
