package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;
import javax.swing.text.Highlighter.HighlightPainter;

import com.google.common.collect.Sets;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.FieldMatches;
import cuchaz.enigma.gui.ClassSelector.ClassSelectionListener;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import de.sciss.syntaxpane.DefaultSyntaxKit;


public class FieldMatchingGui {
	
	public static interface SaveListener {
		public void save(FieldMatches matches);
	}
	
	// controls
	private JFrame m_frame;
	private ClassSelector m_sourceClasses;
	private CodeReader m_sourceReader;
	private CodeReader m_destReader;
	private JButton m_matchButton;
	private JLabel m_sourceLabel;
	private JLabel m_destLabel;
	private HighlightPainter m_unmatchedHighlightPainter;
	private HighlightPainter m_matchedHighlightPainter;

	private ClassMatches m_classMatches;
	private FieldMatches m_fieldMatches;
	private Deobfuscator m_sourceDeobfuscator;
	private Deobfuscator m_destDeobfuscator;
	private SaveListener m_saveListener;
	private ClassEntry m_obfSourceClass;
	private ClassEntry m_obfDestClass;
	private FieldEntry m_obfSourceField;
	private FieldEntry m_obfDestField;
	private Set<FieldEntry> m_obfUnmatchedSourceFields;
	private Set<FieldEntry> m_obfUnmatchedDestFields;

	public FieldMatchingGui(ClassMatches classMatches, FieldMatches fieldMatches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		
		m_classMatches = classMatches;
		m_fieldMatches = fieldMatches;
		m_sourceDeobfuscator = sourceDeobfuscator;
		m_destDeobfuscator = destDeobfuscator;
		
		// init frame
		m_frame = new JFrame(Constants.Name + " - Field Matcher");
		final Container pane = m_frame.getContentPane();
		pane.setLayout(new BorderLayout());
		
		// init classes side
		JPanel classesPanel = new JPanel();
		classesPanel.setLayout(new BoxLayout(classesPanel, BoxLayout.PAGE_AXIS));
		classesPanel.setPreferredSize(new Dimension(200, 0));
		pane.add(classesPanel, BorderLayout.WEST);
		classesPanel.add(new JLabel("Classes"));
		
		m_sourceClasses = new ClassSelector(ClassSelector.DeobfuscatedClassEntryComparator);
		m_sourceClasses.setListener(new ClassSelectionListener() {
			@Override
			public void onSelectClass(ClassEntry classEntry) {
				setSourceClass(classEntry);
			}
		});
		JScrollPane sourceScroller = new JScrollPane(m_sourceClasses);
		classesPanel.add(sourceScroller);

		// init readers
		DefaultSyntaxKit.initKit();
		m_sourceReader = new CodeReader();
		m_sourceReader.setSelectionListener(new CodeReader.SelectionListener() {
			@Override
			public void onSelect(EntryReference<Entry,Entry> reference) {
				if (reference != null) {
					onSelectSource(reference.entry);
				} else {
					onSelectSource(null);
				}
			}
		});
		m_destReader = new CodeReader();
		m_destReader.setSelectionListener(new CodeReader.SelectionListener() {
			@Override
			public void onSelect(EntryReference<Entry,Entry> reference) {
				if (reference != null) {
					onSelectDest(reference.entry);
				} else {
					onSelectDest(null);
				}
			}
		});

		// init all the splits
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(m_sourceReader), new JScrollPane(m_destReader));
		splitRight.setResizeWeight(0.5); // resize 50:50
		JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, classesPanel, splitRight);
		splitLeft.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitLeft, BorderLayout.CENTER);
		splitLeft.resetToPreferredSizes();
		
		// init bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());
		pane.add(bottomPanel, BorderLayout.SOUTH);
		
		m_matchButton = new JButton("Match");
		m_matchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				match();
			}
		});
		
		m_sourceLabel = new JLabel();
		bottomPanel.add(m_sourceLabel);
		bottomPanel.add(m_matchButton);
		m_destLabel = new JLabel();
		bottomPanel.add(m_destLabel);

		// show the frame
		pane.doLayout();
		m_frame.setSize(1024, 576);
		m_frame.setMinimumSize(new Dimension(640, 480));
		m_frame.setVisible(true);
		m_frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		m_unmatchedHighlightPainter = new ObfuscatedHighlightPainter();
		m_matchedHighlightPainter = new DeobfuscatedHighlightPainter();

		// init state
		m_saveListener = null;
		m_obfSourceClass = null;
		m_obfDestClass = null;
		m_obfSourceField = null;
		m_obfDestField = null;
		m_obfUnmatchedSourceFields = null;
		m_obfUnmatchedDestFields = null;
		updateSourceClasses();
		updateMatchButton();
	}

	public void setSaveListener(SaveListener val) {
		m_saveListener = val;
	}
	
	private void updateSourceClasses() {
		m_sourceClasses.setClasses(m_fieldMatches.getSourceClassesWithUnmatchedFields());
		m_sourceClasses.expandAll();
	}
	
	protected void setSourceClass(ClassEntry obfSourceClass) {
		
		m_obfSourceClass = obfSourceClass;
		m_obfDestClass = m_classMatches.getUniqueMatches().get(obfSourceClass);
		if (m_obfDestClass == null) {
			throw new Error("No matching dest class for source class: " + m_obfSourceClass);
		}
		
		updateUnmatchedFields();
		m_sourceReader.decompileClass(m_obfSourceClass, m_sourceDeobfuscator, new Runnable() {
			@Override
			public void run() {
				updateSourceHighlights();
			}
		});
		m_destReader.decompileClass(m_obfDestClass, m_destDeobfuscator, new Runnable() {
			@Override
			public void run() {
				updateDestHighlights();
			}
		});
	}
	
	private void updateUnmatchedFields() {
		m_obfUnmatchedSourceFields = Sets.newHashSet(m_fieldMatches.getUnmatchedSourceFields(m_obfSourceClass));
		m_obfUnmatchedDestFields = Sets.newHashSet();
		for (FieldEntry destFieldEntry : m_destDeobfuscator.getJarIndex().getObfFieldEntries(m_obfDestClass)) {
			if (!m_fieldMatches.isDestMatched(destFieldEntry)) {
				m_obfUnmatchedDestFields.add(destFieldEntry);
			}
		}
	}

	protected void updateSourceHighlights() {
		highlightFields(m_sourceReader, m_sourceDeobfuscator, m_obfUnmatchedSourceFields, m_fieldMatches.matches().keySet());
	}

	protected void updateDestHighlights() {
		highlightFields(m_destReader, m_destDeobfuscator, m_obfUnmatchedDestFields, m_fieldMatches.matches().values());
	}
	
	private void highlightFields(CodeReader reader, Deobfuscator deobfuscator, Collection<FieldEntry> obfFieldEntries, Collection<FieldEntry> obfMatchedFieldEntries) {
		reader.clearHighlights();
		SourceIndex index = reader.getSourceIndex();
		for (FieldEntry obfFieldEntry : obfFieldEntries) {
			FieldEntry deobfFieldEntry = deobfuscator.deobfuscateEntry(obfFieldEntry);
			Token token = index.getDeclarationToken(deobfFieldEntry);
			if (token == null) {
				System.err.println("WARNING: Can't find declaration token for " + deobfFieldEntry);
			} else {
				reader.setHighlightedToken(
					token,
					obfMatchedFieldEntries.contains(obfFieldEntry) ? m_matchedHighlightPainter : m_unmatchedHighlightPainter
				);
			}
		}
	}
	
	protected void onSelectSource(Entry entry) {
		m_sourceLabel.setText("");
		m_obfSourceField = null;
		if (entry != null && entry instanceof FieldEntry) {
			FieldEntry fieldEntry = (FieldEntry)entry;
			FieldEntry obfFieldEntry = m_sourceDeobfuscator.obfuscateEntry(fieldEntry);
			if (m_obfUnmatchedSourceFields.contains(obfFieldEntry)) {
				m_obfSourceField = obfFieldEntry;
				m_sourceLabel.setText(fieldEntry.getName() + " " + fieldEntry.getType().toString());
			}
		}
		updateMatchButton();
	}

	protected void onSelectDest(Entry entry) {
		m_destLabel.setText("");
		m_obfDestField = null;
		if (entry != null && entry instanceof FieldEntry) {
			FieldEntry fieldEntry = (FieldEntry)entry;
			FieldEntry obfFieldEntry = m_destDeobfuscator.obfuscateEntry(fieldEntry);
			if (m_obfUnmatchedDestFields.contains(obfFieldEntry)) {
				m_obfDestField = obfFieldEntry;
				m_destLabel.setText(fieldEntry.getName() + " " + fieldEntry.getType().toString());
			}
		}
		updateMatchButton();
	}
		
	private void updateMatchButton() {
		m_matchButton.setEnabled(m_obfSourceField != null && m_obfDestField != null);
	}
	
	protected void match() {
		
		// update the field matches
		m_fieldMatches.makeMatch(m_obfSourceField, m_obfDestField);
		save();

		// update the ui
		onSelectSource(null);
		onSelectDest(null);
		updateUnmatchedFields();
		updateSourceHighlights();
		updateDestHighlights();
		updateSourceClasses();
	}

	private void save() {
		if (m_saveListener != null) {
			m_saveListener.save(m_fieldMatches);
		}
	}
}
