package cuchaz.enigma.gui;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;

import com.strobel.decompiler.languages.java.ast.CompilationUnit;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import de.sciss.syntaxpane.DefaultSyntaxKit;


public class CodeReader extends JEditorPane {
	
	private static final long serialVersionUID = 3673180950485748810L;
	
	private static final Object m_lock = new Object();
	
	private SelectionHighlightPainter m_highlightPainter;
	private SourceIndex m_sourceIndex;
	
	public CodeReader() {
		
		setEditable(false);
		setContentType("text/java");
		
		// turn off token highlighting (it's wrong most of the time anyway...)
		DefaultSyntaxKit kit = (DefaultSyntaxKit)getEditorKit();
		kit.toggleComponent(this, "de.sciss.syntaxpane.components.TokenMarker");
		
		m_highlightPainter = new SelectionHighlightPainter();
		m_sourceIndex = null;
	}
	
	public void setCode(String code) {
		// sadly, the java lexer is not thread safe, so we have to serialize all these calls
		synchronized (m_lock) {
			setText(code);
		}
	}
	
	public void decompileClass(ClassEntry classEntry, Deobfuscator deobfuscator) {
		decompileClass(classEntry, deobfuscator, null);
	}
	
	public void decompileClass(final ClassEntry classEntry, final Deobfuscator deobfuscator, final Runnable callback) {
		
		if (classEntry == null) {
			setCode(null);
			return;
		}
		
		setCode("(decompiling...)");

		// run decompilation in a separate thread to keep ui responsive
		new Thread() {
			@Override
			public void run() {
				
				// get the outermost class
				ClassEntry outermostClassEntry = classEntry;
				while (outermostClassEntry.isInnerClass()) {
					outermostClassEntry = outermostClassEntry.getOuterClassEntry();
				}
				
				// decompile it
				CompilationUnit sourceTree = deobfuscator.getSourceTree(outermostClassEntry.getName());
				String source = deobfuscator.getSource(sourceTree);
				setCode(source);
				m_sourceIndex = deobfuscator.getSourceIndex(sourceTree, source);
				
				if (callback != null) {
					callback.run();
				}
			}
		}.start();
	}
	
	public void navigateToClassDeclaration(ClassEntry classEntry) {

		// navigate to the class declaration
		Token token = m_sourceIndex.getDeclarationToken(classEntry);
		if (token == null) {
			// couldn't find the class declaration token, might be an anonymous class
			// look for any declaration in that class instead
			for (Entry entry : m_sourceIndex.declarations()) {
				if (entry.getClassEntry().equals(classEntry)) {
					token = m_sourceIndex.getDeclarationToken(entry);
					break;
				}
			}
		}
		
		if (token != null) {
			navigateToToken(token);
		} else {
			// couldn't find anything =(
			System.out.println("Unable to find declaration in source for " + classEntry);
		}
	}
	
	public void navigateToToken(final Token token) {
		navigateToToken(this, token, m_highlightPainter);
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
