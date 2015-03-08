/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;

import cuchaz.enigma.analysis.Token;

public class GuiTricks {
	
	public static JLabel unboldLabel(JLabel label) {
		Font font = label.getFont();
		label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
		return label;
	}
	
	public static void showToolTipNow(JComponent component) {
		// HACKHACK: trick the tooltip manager into showing the tooltip right now
		ToolTipManager manager = ToolTipManager.sharedInstance();
		int oldDelay = manager.getInitialDelay();
		manager.setInitialDelay(0);
		manager.mouseMoved(new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 0, 0, 0, false));
		manager.setInitialDelay(oldDelay);
	}
	
	public static void navigateToToken(final JEditorPane editor, final Token token, final HighlightPainter highlightPainter) {
		
		// set the caret position to the token
		editor.setCaretPosition(token.start);
		editor.grabFocus();
		
		try {
			// make sure the token is visible in the scroll window
			Rectangle start = editor.modelToView(token.start);
			Rectangle end = editor.modelToView(token.end);
			final Rectangle show = start.union(end);
			show.grow(start.width * 10, start.height * 6);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					editor.scrollRectToVisible(show);
				}
			});
		} catch (BadLocationException ex) {
			throw new Error(ex);
		}
		
		// highlight the token momentarily
		final Timer timer = new Timer(200, new ActionListener() {
			private int m_counter = 0;
			private Object m_highlight = null;
			
			@Override
			public void actionPerformed(ActionEvent event) {
				if (m_counter % 2 == 0) {
					try {
						m_highlight = editor.getHighlighter().addHighlight(token.start, token.end, highlightPainter);
					} catch (BadLocationException ex) {
						// don't care
					}
				} else if (m_highlight != null) {
					editor.getHighlighter().removeHighlight(m_highlight);
				}
				
				if (m_counter++ > 6) {
					Timer timer = (Timer)event.getSource();
					timer.stop();
				}
			}
		});
		timer.start();
	}
}
