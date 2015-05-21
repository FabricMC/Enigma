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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.MemberMatches;
import cuchaz.enigma.gui.ClassSelector.ClassSelectionListener;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import de.sciss.syntaxpane.DefaultSyntaxKit;


public class MemberMatchingGui<T extends Entry> {
	
	private static enum SourceType {
		Matched {
			
			@Override
			public <T extends Entry> Collection<ClassEntry> getObfSourceClasses(MemberMatches<T> matches) {
				return matches.getSourceClassesWithoutUnmatchedEntries();
			}
		},
		Unmatched {
			
			@Override
			public <T extends Entry> Collection<ClassEntry> getObfSourceClasses(MemberMatches<T> matches) {
				return matches.getSourceClassesWithUnmatchedEntries();
			}
		};
		
		public JRadioButton newRadio(ActionListener listener, ButtonGroup group) {
			JRadioButton button = new JRadioButton(name(), this == getDefault());
			button.setActionCommand(name());
			button.addActionListener(listener);
			group.add(button);
			return button;
		}
		
		public abstract <T extends Entry> Collection<ClassEntry> getObfSourceClasses(MemberMatches<T> matches);
		
		public static SourceType getDefault() {
			return values()[0];
		}
	}
	
	public static interface SaveListener<T extends Entry> {
		public void save(MemberMatches<T> matches);
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
	private MemberMatches<T> m_memberMatches;
	private Deobfuscator m_sourceDeobfuscator;
	private Deobfuscator m_destDeobfuscator;
	private SaveListener<T> m_saveListener;
	private SourceType m_sourceType;
	private ClassEntry m_obfSourceClass;
	private ClassEntry m_obfDestClass;
	private T m_obfSourceEntry;
	private T m_obfDestEntry;

	public MemberMatchingGui(ClassMatches classMatches, MemberMatches<T> fieldMatches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		
		m_classMatches = classMatches;
		m_memberMatches = fieldMatches;
		m_sourceDeobfuscator = sourceDeobfuscator;
		m_destDeobfuscator = destDeobfuscator;
		
		// init frame
		m_frame = new JFrame(Constants.Name + " - Member Matcher");
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
		m_obfSourceEntry = null;
		m_obfDestEntry = null;
		setSourceType(SourceType.getDefault());
		updateButtons();
	}

	protected void setSourceType(SourceType val) {
		m_sourceType = val;
		updateSourceClasses();
	}

	public void setSaveListener(SaveListener<T> val) {
		m_saveListener = val;
	}
	
	private void updateSourceClasses() {
		
		String selectedPackage = m_sourceClasses.getSelectedPackage();
		
		List<ClassEntry> deobfClassEntries = Lists.newArrayList();
		for (ClassEntry entry : m_sourceType.getObfSourceClasses(m_memberMatches)) {
			deobfClassEntries.add(m_sourceDeobfuscator.deobfuscateEntry(entry));
		}
		m_sourceClasses.setClasses(deobfClassEntries);
		
		if (selectedPackage != null) {
			m_sourceClasses.expandPackage(selectedPackage);
		}
		
		for (SourceType sourceType : SourceType.values()) {
			m_sourceTypeButtons.get(sourceType).setText(String.format("%s (%d)",
				sourceType.name(), sourceType.getObfSourceClasses(m_memberMatches).size()
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
		highlightEntries(m_sourceReader, m_sourceDeobfuscator, m_memberMatches.matches().keySet(), m_memberMatches.getUnmatchedSourceEntries());
	}

	protected void updateDestHighlights() {
		highlightEntries(m_destReader, m_destDeobfuscator, m_memberMatches.matches().values(), m_memberMatches.getUnmatchedDestEntries());
	}
	
	private void highlightEntries(CodeReader reader, Deobfuscator deobfuscator, Collection<T> obfMatchedEntries, Collection<T> obfUnmatchedEntries) {
		reader.clearHighlights();
		SourceIndex index = reader.getSourceIndex();
		
		// matched fields
		for (T obfT : obfMatchedEntries) {
			T deobfT = deobfuscator.deobfuscateEntry(obfT);
			Token token = index.getDeclarationToken(deobfT);
			if (token != null) {
				reader.setHighlightedToken(token, m_matchedHighlightPainter);
			}
		}
		
		// unmatched fields
		for (T obfT : obfUnmatchedEntries) {
			T deobfT = deobfuscator.deobfuscateEntry(obfT);
			Token token = index.getDeclarationToken(deobfT);
			if (token != null) {
				reader.setHighlightedToken(token, m_unmatchedHighlightPainter);
			}
		}
	}
	
	private boolean isSelectionMatched() {
		return m_obfSourceEntry != null && m_obfDestEntry != null
			&& m_memberMatches.isMatched(m_obfSourceEntry, m_obfDestEntry);
	}
	
	protected void onSelectSource(Entry source) {

		// start with no selection
		if (isSelectionMatched()) {
			setDest(null);
		}
		setSource(null);
		
		// then look for a valid source selection
		if (source != null) {
			
			// this looks really scary, but it's actually ok
			// Deobfuscator.obfuscateEntry can handle all implementations of Entry
			// and MemberMatches.hasSource() will only pass entries that actually match T
			@SuppressWarnings("unchecked")
			T sourceEntry = (T)source;

			T obfSourceEntry = m_sourceDeobfuscator.obfuscateEntry(sourceEntry);
			if (m_memberMatches.hasSource(obfSourceEntry)) {
				setSource(obfSourceEntry);
				
				// look for a matched dest too
				T obfDestEntry = m_memberMatches.matches().get(obfSourceEntry);
				if (obfDestEntry != null) {
					setDest(obfDestEntry);
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
		if (dest != null) {
			
			// this looks really scary, but it's actually ok
			// Deobfuscator.obfuscateEntry can handle all implementations of Entry
			// and MemberMatches.hasSource() will only pass entries that actually match T
			@SuppressWarnings("unchecked")
			T destEntry = (T)dest;
			
			T obfDestEntry = m_destDeobfuscator.obfuscateEntry(destEntry);
			if (m_memberMatches.hasDest(obfDestEntry)) {
				setDest(obfDestEntry);

				// look for a matched source too
				T obfSourceEntry = m_memberMatches.matches().inverse().get(obfDestEntry);
				if (obfSourceEntry != null) {
					setSource(obfSourceEntry);
				}
			}
		}
		
		updateButtons();
	}
	
	private void setSource(T obfEntry) {
		if (obfEntry == null) {
			m_obfSourceEntry = obfEntry;
			m_sourceLabel.setText("");
		} else {
			m_obfSourceEntry = obfEntry;
			m_sourceLabel.setText(getEntryLabel(obfEntry, m_sourceDeobfuscator));
		}
	}
	
	private void setDest(T obfEntry) {
		if (obfEntry == null) {
			m_obfDestEntry = obfEntry;
			m_destLabel.setText("");
		} else {
			m_obfDestEntry = obfEntry;
			m_destLabel.setText(getEntryLabel(obfEntry, m_destDeobfuscator));
		}
	}

	private String getEntryLabel(T obfEntry, Deobfuscator deobfuscator) {
		// show obfuscated and deobfuscated names, but no types/signatures
		T deobfEntry = deobfuscator.deobfuscateEntry(obfEntry);
		return String.format("%s (%s)", deobfEntry.getName(), obfEntry.getName());
	}

	private void updateButtons() {
		
		GuiTricks.deactivateButton(m_matchButton);
		GuiTricks.deactivateButton(m_unmatchableButton);
		
		if (m_obfSourceEntry != null && m_obfDestEntry != null) {
			if (m_memberMatches.isMatched(m_obfSourceEntry, m_obfDestEntry)) {
				GuiTricks.activateButton(m_matchButton, "Unmatch", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						unmatch();
					}
				});
			} else if (!m_memberMatches.isMatchedSourceEntry(m_obfSourceEntry) && !m_memberMatches.isMatchedDestEntry(m_obfDestEntry)) {
				GuiTricks.activateButton(m_matchButton, "Match", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						match();
					}
				});
			}
		} else if (m_obfSourceEntry != null) {
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
		m_memberMatches.makeMatch(m_obfSourceEntry, m_obfDestEntry);
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
		m_memberMatches.unmakeMatch(m_obfSourceEntry, m_obfDestEntry);
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
		m_memberMatches.makeSourceUnmatchable(m_obfSourceEntry);
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
			m_saveListener.save(m_memberMatches);
		}
	}
}
