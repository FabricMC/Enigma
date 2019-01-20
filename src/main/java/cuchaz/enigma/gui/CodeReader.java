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

import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

public class CodeReader extends JEditorPane {

	private static final long serialVersionUID = 3673180950485748810L;

	private static final Object lock = new Object();
	private SourceIndex sourceIndex;
	private SelectionListener selectionListener;

	public CodeReader() {

		setEditable(false);
		setContentType("text/java");

		// turn off token highlighting (it's wrong most of the time anyway...)
		DefaultSyntaxKit kit = (DefaultSyntaxKit) getEditorKit();
		kit.toggleComponent(this, "de.sciss.syntaxpane.components.TokenMarker");

		// hook events
		addCaretListener(event ->
		{
			if (selectionListener != null && sourceIndex != null) {
				Token token = sourceIndex.getReferenceToken(event.getDot());
				if (token != null) {
					Collection<EntryReference<Entry<?>, Entry<?>>> references = sourceIndex.getDeobfReferences(token);
					references.stream().findFirst().ifPresent(selectionListener::onSelect);
				} else {
					selectionListener.onSelect(null);
				}
			}
		});
	}

	// HACKHACK: someday we can update the main GUI to use this code reader
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

	public void setSelectionListener(SelectionListener val) {
		selectionListener = val;
	}

	public void setCode(String code) {
		// sadly, the java lexer is not thread safe, so we have to serialize all these calls
		synchronized (lock) {
			setText(code);
		}
	}

	public SourceIndex getSourceIndex() {
		return sourceIndex;
	}

	public void decompileClass(ClassEntry classEntry, Deobfuscator deobfuscator) {
		decompileClass(classEntry, deobfuscator, null);
	}

	public void decompileClass(ClassEntry classEntry, Deobfuscator deobfuscator, Runnable callback) {
		decompileClass(classEntry, deobfuscator, null, callback);
	}

	public void decompileClass(final ClassEntry classEntry, final Deobfuscator deobfuscator, final Boolean ignoreBadTokens, final Runnable callback) {

		if (classEntry == null) {
			setCode(null);
			return;
		}

		setCode("(decompiling...)");

		// run decompilation in a separate thread to keep ui responsive
		new Thread(() ->
		{

			// decompile it

			CompilationUnit sourceTree = deobfuscator.getSourceTree(classEntry.getName());
			String source = deobfuscator.getSource(sourceTree);
			setCode(source);
			sourceIndex = deobfuscator.getSourceIndex(sourceTree, source, ignoreBadTokens);

			if (callback != null) {
				callback.run();
			}
		}).start();
	}

	public interface SelectionListener {
		void onSelect(EntryReference<Entry<?>, Entry<?>> reference);
	}
}
