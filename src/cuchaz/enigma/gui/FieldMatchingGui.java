package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;
import javax.swing.text.Highlighter.HighlightPainter;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

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
	
	private static enum SourceType {
		Matched {
			
			@Override
			public Collection<ClassEntry> getObfSourceClasses(FieldMatches matches) {
				return matches.getSourceClassesWithoutUnmatchedFields();
			}
		},
		Unmatched {
			
			@Override
			public Collection<ClassEntry> getObfSourceClasses(FieldMatches matches) {
				return matches.getSourceClassesWithUnmatchedFields();
			}
		};
		
		public JRadioButton newRadio(ActionListener listener, ButtonGroup group) {
			JRadioButton button = new JRadioButton(name(), this == getDefault());
			button.setActionCommand(name());
			button.addActionListener(listener);
			group.add(button);
			return button;
		}
		
		public abstract Collection<ClassEntry> getObfSourceClasses(FieldMatches matches);
		
		public static SourceType getDefault() {
			return values()[0];
		}
	}
	
	public static interface SaveListener {
		public void save(FieldMatches matches);
	}
	
	// controls
	private JFrame m_frame;
	private Map<SourceType,JRadioButton> m_sourceTypeButtons;
	private ClassSelector m_sourceClasses;
	private CodeReader m_sourceReader;
	private CodeReader m_destReader;
	private JButton m_matchButton;
	private JButton m_unmatchableButton;
	private JLabel m_sourceLabel;
	private JLabel m_destLabel;
	private HighlightPainter m_unmatchedHighlightPainter;
	private HighlightPainter m_matchedHighlightPainter;

	private ClassMatches m_classMatches;
	private FieldMatches m_fieldMatches;
	private Deobfuscator m_sourceDeobfuscator;
	private Deobfuscator m_destDeobfuscator;
	private SaveListener m_saveListener;
	private SourceType m_sourceType;
	private ClassEntry m_obfSourceClass;
	private ClassEntry m_obfDestClass;
	private FieldEntry m_obfSourceField;
	private FieldEntry m_obfDestField;

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
		
		// init source type radios
		JPanel sourceTypePanel = new JPanel();
		classesPanel.add(sourceTypePanel);
		sourceTypePanel.setLayout(new BoxLayout(sourceTypePanel, BoxLayout.PAGE_AXIS));
		ActionListener sourceTypeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				setSourceType(SourceType.valueOf(event.getActionCommand()));
			}
		};
		ButtonGroup sourceTypeButtons = new ButtonGroup();
		m_sourceTypeButtons = Maps.newHashMap();
		for (SourceType sourceType : SourceType.values()) {
			JRadioButton button = sourceType.newRadio(sourceTypeListener, sourceTypeButtons);
			m_sourceTypeButtons.put(sourceType, button);
			sourceTypePanel.add(button);
		}
		
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
		
		// add key bindings
		KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_M:
						m_matchButton.doClick();
					break;
				}
			}
		};
		m_sourceReader.addKeyListener(keyListener);
		m_destReader.addKeyListener(keyListener);

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
		
		m_matchButton = new JButton();
		m_unmatchableButton = new JButton();
		
		m_sourceLabel = new JLabel();
		bottomPanel.add(m_sourceLabel);
		bottomPanel.add(m_matchButton);
		bottomPanel.add(m_unmatchableButton);
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
		setSourceType(SourceType.getDefault());
		updateButtons();
	}

	protected void setSourceType(SourceType val) {
		m_sourceType = val;
		updateSourceClasses();
	}

	public void setSaveListener(SaveListener val) {
		m_saveListener = val;
	}
	
	private void updateSourceClasses() {
		
		String selectedPackage = m_sourceClasses.getSelectedPackage();
		
		List<ClassEntry> deobfClassEntries = Lists.newArrayList();
		for (ClassEntry entry : m_sourceType.getObfSourceClasses(m_fieldMatches)) {
			deobfClassEntries.add(m_sourceDeobfuscator.deobfuscateEntry(entry));
		}
		m_sourceClasses.setClasses(deobfClassEntries);
		
		if (selectedPackage != null) {
			m_sourceClasses.expandPackage(selectedPackage);
		}
		
		for (SourceType sourceType : SourceType.values()) {
			m_sourceTypeButtons.get(sourceType).setText(String.format("%s (%d)",
				sourceType.name(), sourceType.getObfSourceClasses(m_fieldMatches).size()
			));
		}
	}
	
	protected void setSourceClass(ClassEntry sourceClass) {
		
		m_obfSourceClass = m_sourceDeobfuscator.obfuscateEntry(sourceClass);
		m_obfDestClass = m_classMatches.getUniqueMatches().get(m_obfSourceClass);
		if (m_obfDestClass == null) {
			throw new Error("No matching dest class for source class: " + m_obfSourceClass);
		}
		
		m_sourceReader.decompileClass(m_obfSourceClass, m_sourceDeobfuscator, false, new Runnable() {
			@Override
			public void run() {
				updateSourceHighlights();
			}
		});
		m_destReader.decompileClass(m_obfDestClass, m_destDeobfuscator, false, new Runnable() {
			@Override
			public void run() {
				updateDestHighlights();
			}
		});
	}
	
	protected void updateSourceHighlights() {
		highlightFields(m_sourceReader, m_sourceDeobfuscator, m_fieldMatches.matches().keySet(), m_fieldMatches.getUnmatchedSourceFields());
	}

	protected void updateDestHighlights() {
		highlightFields(m_destReader, m_destDeobfuscator, m_fieldMatches.matches().values(), m_fieldMatches.getUnmatchedDestFields());
	}
	
	private void highlightFields(CodeReader reader, Deobfuscator deobfuscator, Collection<FieldEntry> obfMatchedFields, Collection<FieldEntry> obfUnmatchedFields) {
		reader.clearHighlights();
		SourceIndex index = reader.getSourceIndex();
		
		// matched fields
		for (FieldEntry obfFieldEntry : obfMatchedFields) {
			FieldEntry deobfFieldEntry = deobfuscator.deobfuscateEntry(obfFieldEntry);
			Token token = index.getDeclarationToken(deobfFieldEntry);
			if (token != null) {
				reader.setHighlightedToken(token, m_matchedHighlightPainter);
			}
		}
		
		// unmatched fields
		for (FieldEntry obfFieldEntry : obfUnmatchedFields) {
			FieldEntry deobfFieldEntry = deobfuscator.deobfuscateEntry(obfFieldEntry);
			Token token = index.getDeclarationToken(deobfFieldEntry);
			if (token != null) {
				reader.setHighlightedToken(token, m_unmatchedHighlightPainter);
			}
		}
	}
	
	private boolean isSelectionMatched() {
		return m_obfSourceField != null && m_obfDestField != null
			&& m_fieldMatches.isMatched(m_obfSourceField, m_obfDestField);
	}
	
	protected void onSelectSource(Entry source) {

		// start with no selection
		if (isSelectionMatched()) {
			setDest(null);
		}
		setSource(null);
		
		// then look for a valid source selection
		if (source != null && source instanceof FieldEntry) {
			FieldEntry sourceField = (FieldEntry)source;
			FieldEntry obfSourceField = m_sourceDeobfuscator.obfuscateEntry(sourceField);
			if (m_fieldMatches.hasSource(obfSourceField)) {
				setSource(obfSourceField);
				
				// look for a matched dest too
				FieldEntry obfDestField = m_fieldMatches.matches().get(obfSourceField);
				if (obfDestField != null) {
					setDest(obfDestField);
				}
			}
		}
		
		updateButtons();
	}

	protected void onSelectDest(Entry dest) {
		
		// start with no selection
		if (isSelectionMatched()) {
			setSource(null);
		}
		setDest(null);

		// then look for a valid dest selection
		if (dest != null && dest instanceof FieldEntry) {
			FieldEntry destField = (FieldEntry)dest;
			FieldEntry obfDestField = m_destDeobfuscator.obfuscateEntry(destField);
			if (m_fieldMatches.hasDest(obfDestField)) {
				setDest(obfDestField);

				// look for a matched source too
				FieldEntry obfSourceField = m_fieldMatches.matches().inverse().get(obfDestField);
				if (obfSourceField != null) {
					setSource(obfSourceField);
				}
			}
		}
		
		updateButtons();
	}
	
	private void setSource(FieldEntry obfField) {
		if (obfField == null) {
			m_obfSourceField = obfField;
			m_sourceLabel.setText("");
		} else {
			m_obfSourceField = obfField;
			FieldEntry deobfField = m_sourceDeobfuscator.deobfuscateEntry(obfField);
			m_sourceLabel.setText(deobfField.getName() + " " + deobfField.getType().toString());
		}
	}
	
	private void setDest(FieldEntry obfField) {
		if (obfField == null) {
			m_obfDestField = obfField;
			m_destLabel.setText("");
		} else {
			m_obfDestField = obfField;
			FieldEntry deobfField = m_destDeobfuscator.deobfuscateEntry(obfField);
			m_destLabel.setText(deobfField.getName() + " " + deobfField.getType().toString());
		}
	}

	private void updateButtons() {
		
		GuiTricks.deactivateButton(m_matchButton);
		GuiTricks.deactivateButton(m_unmatchableButton);
		
		if (m_obfSourceField != null && m_obfDestField != null) {
			if (m_fieldMatches.isMatched(m_obfSourceField, m_obfDestField)) {
				GuiTricks.activateButton(m_matchButton, "Unmatch", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						unmatch();
					}
				});
			} else if (!m_fieldMatches.isMatchedSourceField(m_obfSourceField) && !m_fieldMatches.isMatchedDestField(m_obfDestField)) {
				GuiTricks.activateButton(m_matchButton, "Match", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						match();
					}
				});
			}
		} else if (m_obfSourceField != null) {
			GuiTricks.activateButton(m_unmatchableButton, "Set Unmatchable", new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					unmatchable();
				}
			});
		}
	}
	
	protected void match() {
		
		// update the field matches
		m_fieldMatches.makeMatch(m_obfSourceField, m_obfDestField);
		save();

		// update the ui
		onSelectSource(null);
		onSelectDest(null);
		updateSourceHighlights();
		updateDestHighlights();
		updateSourceClasses();
	}
	
	protected void unmatch() {
		
		// update the field matches
		m_fieldMatches.unmakeMatch(m_obfSourceField, m_obfDestField);
		save();

		// update the ui
		onSelectSource(null);
		onSelectDest(null);
		updateSourceHighlights();
		updateDestHighlights();
		updateSourceClasses();
	}
	
	protected void unmatchable() {
		
		// update the field matches
		m_fieldMatches.makeSourceUnmatchable(m_obfSourceField);
		save();
		
		// update the ui
		onSelectSource(null);
		onSelectDest(null);
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
