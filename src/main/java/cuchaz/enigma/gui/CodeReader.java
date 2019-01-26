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

package cuchaz.enigma.gui;

import cuchaz.enigma.analysis.Token;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CodeReader extends JEditorPane {
	private static final long serialVersionUID = 3673180950485748810L;

	// HACKHACK: someday we can update the main GUI to use this code reader
	public static void navigateToToken(final JEditorPane editor, final Token token, final HighlightPainter highlightPainter) {

		// set the caret position to the token
		Document document = editor.getDocument();
		int clampedPosition = Math.min(Math.max(token.start, 0), document.getLength());

		editor.setCaretPosition(clampedPosition);
		editor.grabFocus();

		try {
			// make sure the token is visible in the scroll window
			Rectangle start = editor.modelToView(token.start);
			Rectangle end = editor.modelToView(token.end);
			final Rectangle show = start.union(end);
			show.grow(start.width * 10, start.height * 6);
			SwingUtilities.invokeLater(() -> editor.scrollRectToVisible(show));
		} catch (BadLocationException ex) {
			throw new Error(ex);
		}

		// highlight the token momentarily
		final Timer timer = new Timer(200, new ActionListener() {
			private int counter = 0;
			private Object highlight = null;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (counter % 2 == 0) {
					try {
						highlight = editor.getHighlighter().addHighlight(token.start, token.end, highlightPainter);
					} catch (BadLocationException ex) {
						// don't care
					}
				} else if (highlight != null) {
					editor.getHighlighter().removeHighlight(highlight);
				}

				if (counter++ > 6) {
					Timer timer = (Timer) event.getSource();
					timer.stop();
				}
			}
		});
		timer.start();
	}
}
