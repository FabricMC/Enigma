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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.MemberMatches;
import cuchaz.enigma.gui.highlight.DeobfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.ObfuscatedHighlightPainter;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MemberMatchingGui<T extends Entry> {

	// controls
	private JFrame frame;
	private Map<SourceType, JRadioButton> sourceTypeButtons;
	private ClassSelector sourceClasses;
	private CodeReader sourceReader;
	private CodeReader destReader;
	private JButton matchButton;
	private JButton unmatchableButton;
	private JLabel sourceLabel;
	private JLabel destLabel;
	private HighlightPainter unmatchedHighlightPainter;
	private HighlightPainter matchedHighlightPainter;
	private ClassMatches classMatches;
	private MemberMatches<T> memberMatches;
	private Deobfuscator sourceDeobfuscator;
	private Deobfuscator destDeobfuscator;
	private SaveListener<T> saveListener;
	private SourceType sourceType;
	private ClassEntry obfSourceClass;
	private ClassEntry obfDestClass;
	private T obfSourceEntry;
	private T obfDestEntry;

	public MemberMatchingGui(ClassMatches classMatches, MemberMatches<T> fieldMatches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {

		this.classMatches = classMatches;
		memberMatches = fieldMatches;
		this.sourceDeobfuscator = sourceDeobfuscator;
		this.destDeobfuscator = destDeobfuscator;

		// init frame
		frame = new JFrame(Constants.NAME + " - Member Matcher");
		final Container pane = frame.getContentPane();
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
		ActionListener sourceTypeListener = event -> setSourceType(SourceType.valueOf(event.getActionCommand()));
		ButtonGroup sourceTypeButtons = new ButtonGroup();
		this.sourceTypeButtons = Maps.newHashMap();
		for (SourceType sourceType : SourceType.values()) {
			JRadioButton button = sourceType.newRadio(sourceTypeListener, sourceTypeButtons);
			this.sourceTypeButtons.put(sourceType, button);
			sourceTypePanel.add(button);
		}

		sourceClasses = new ClassSelector(null, ClassSelector.DEOBF_CLASS_COMPARATOR, false);
		sourceClasses.setSelectionListener(this::setSourceClass);
		JScrollPane sourceScroller = new JScrollPane(sourceClasses);
		classesPanel.add(sourceScroller);

		// init readers
		DefaultSyntaxKit.initKit();
		sourceReader = new CodeReader();
		sourceReader.setSelectionListener(reference ->
		{
			if (reference != null) {
				onSelectSource(reference.entry);
			} else {
				onSelectSource(null);
			}
		});
		destReader = new CodeReader();
		destReader.setSelectionListener(reference ->
		{
			if (reference != null) {
				onSelectDest(reference.entry);
			} else {
				onSelectDest(null);
			}
		});

		// add key bindings
		KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_M)
					matchButton.doClick();
			}
		};
		sourceReader.addKeyListener(keyListener);
		destReader.addKeyListener(keyListener);

		// init all the splits
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(sourceReader), new JScrollPane(
			destReader));
		splitRight.setResizeWeight(0.5); // resize 50:50
		JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, classesPanel, splitRight);
		splitLeft.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitLeft, BorderLayout.CENTER);
		splitLeft.resetToPreferredSizes();

		// init bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());
		pane.add(bottomPanel, BorderLayout.SOUTH);

		matchButton = new JButton();
		unmatchableButton = new JButton();

		sourceLabel = new JLabel();
		bottomPanel.add(sourceLabel);
		bottomPanel.add(matchButton);
		bottomPanel.add(unmatchableButton);
		destLabel = new JLabel();
		bottomPanel.add(destLabel);

		// show the frame
		pane.doLayout();
		frame.setSize(1024, 576);
		frame.setMinimumSize(new Dimension(640, 480));
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		unmatchedHighlightPainter = new ObfuscatedHighlightPainter();
		matchedHighlightPainter = new DeobfuscatedHighlightPainter();

		// init state
		saveListener = null;
		obfSourceClass = null;
		obfDestClass = null;
		obfSourceEntry = null;
		obfDestEntry = null;
		setSourceType(SourceType.getDefault());
		updateButtons();
	}

	protected void setSourceType(SourceType val) {
		sourceType = val;
		updateSourceClasses();
	}

	public void setSaveListener(SaveListener<T> val) {
		saveListener = val;
	}

	private void updateSourceClasses() {

		String selectedPackage = sourceClasses.getSelectedPackage();

		List<ClassEntry> deobfClassEntries = Lists.newArrayList();
		for (ClassEntry entry : sourceType.getObfSourceClasses(memberMatches)) {
			deobfClassEntries.add(sourceDeobfuscator.deobfuscateEntry(entry));
		}
		sourceClasses.setClasses(deobfClassEntries);

		if (selectedPackage != null) {
			sourceClasses.expandPackage(selectedPackage);
		}

		for (SourceType sourceType : SourceType.values()) {
			sourceTypeButtons.get(sourceType).setText(String.format("%s (%d)",
				sourceType.name(), sourceType.getObfSourceClasses(memberMatches).size()
			));
		}
	}

	protected void setSourceClass(ClassEntry sourceClass) {

		obfSourceClass = sourceDeobfuscator.obfuscateEntry(sourceClass);
		obfDestClass = classMatches.getUniqueMatches().get(obfSourceClass);
		if (obfDestClass == null) {
			throw new Error("No matching dest class for source class: " + obfSourceClass);
		}

		sourceReader.decompileClass(obfSourceClass, sourceDeobfuscator, false, this::updateSourceHighlights);
		destReader.decompileClass(obfDestClass, destDeobfuscator, false, this::updateDestHighlights);
	}

	protected void updateSourceHighlights() {
		highlightEntries(sourceReader, sourceDeobfuscator, memberMatches.matches().keySet(), memberMatches.getUnmatchedSourceEntries());
	}

	protected void updateDestHighlights() {
		highlightEntries(destReader, destDeobfuscator, memberMatches.matches().values(), memberMatches.getUnmatchedDestEntries());
	}

	private void highlightEntries(CodeReader reader, Deobfuscator deobfuscator, Collection<T> obfMatchedEntries, Collection<T> obfUnmatchedEntries) {
		reader.clearHighlights();
		// matched fields
		updateHighlighted(obfMatchedEntries, deobfuscator, reader, matchedHighlightPainter);
		// unmatched fields
		updateHighlighted(obfUnmatchedEntries, deobfuscator, reader, unmatchedHighlightPainter);
	}

	private void updateHighlighted(Collection<T> entries, Deobfuscator deobfuscator, CodeReader reader, HighlightPainter painter) {
		SourceIndex index = reader.getSourceIndex();
		for (T obfT : entries) {
			T deobfT = deobfuscator.deobfuscateEntry(obfT);
			Token token = index.getDeclarationToken(deobfT);
			if (token != null) {
				reader.setHighlightedToken(token, painter);
			}
		}
	}

	private boolean isSelectionMatched() {
		return obfSourceEntry != null && obfDestEntry != null
			&& memberMatches.isMatched(obfSourceEntry, obfDestEntry);
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
			T sourceEntry = (T) source;

			T obfSourceEntry = sourceDeobfuscator.obfuscateEntry(sourceEntry);
			if (memberMatches.hasSource(obfSourceEntry)) {
				setSource(obfSourceEntry);

				// look for a matched dest too
				T obfDestEntry = memberMatches.matches().get(obfSourceEntry);
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
			T destEntry = (T) dest;

			T obfDestEntry = destDeobfuscator.obfuscateEntry(destEntry);
			if (memberMatches.hasDest(obfDestEntry)) {
				setDest(obfDestEntry);

				// look for a matched source too
				T obfSourceEntry = memberMatches.matches().inverse().get(obfDestEntry);
				if (obfSourceEntry != null) {
					setSource(obfSourceEntry);
				}
			}
		}

		updateButtons();
	}

	private void setSource(T obfEntry) {
		if (obfEntry == null) {
			obfSourceEntry = null;
			sourceLabel.setText("");
		} else {
			obfSourceEntry = obfEntry;
			sourceLabel.setText(getEntryLabel(obfEntry, sourceDeobfuscator));
		}
	}

	private void setDest(T obfEntry) {
		if (obfEntry == null) {
			obfDestEntry = null;
			destLabel.setText("");
		} else {
			obfDestEntry = obfEntry;
			destLabel.setText(getEntryLabel(obfEntry, destDeobfuscator));
		}
	}

	private String getEntryLabel(T obfEntry, Deobfuscator deobfuscator) {
		// show obfuscated and deobfuscated names, but no types/signatures
		T deobfEntry = deobfuscator.deobfuscateEntry(obfEntry);
		return String.format("%s (%s)", deobfEntry.getName(), obfEntry.getName());
	}

	private void updateButtons() {

		GuiTricks.deactivateButton(matchButton);
		GuiTricks.deactivateButton(unmatchableButton);

		if (obfSourceEntry != null && obfDestEntry != null) {
			if (memberMatches.isMatched(obfSourceEntry, obfDestEntry))
				GuiTricks.activateButton(matchButton, "Unmatch", event -> unmatch());
			else if (!memberMatches.isMatchedSourceEntry(obfSourceEntry) && !memberMatches.isMatchedDestEntry(
				obfDestEntry))
				GuiTricks.activateButton(matchButton, "Match", event -> match());
		} else if (obfSourceEntry != null)
			GuiTricks.activateButton(unmatchableButton, "Set Unmatchable", event -> unmatchable());
	}

	protected void match() {

		// update the field matches
		memberMatches.makeMatch(obfSourceEntry, obfDestEntry, sourceDeobfuscator, destDeobfuscator);
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
		memberMatches.unmakeMatch(obfSourceEntry, obfDestEntry, sourceDeobfuscator, destDeobfuscator);
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
		memberMatches.makeSourceUnmatchable(obfSourceEntry, sourceDeobfuscator);
		save();

		// update the ui
		onSelectSource(null);
		onSelectDest(null);
		updateSourceHighlights();
		updateDestHighlights();
		updateSourceClasses();
	}

	private void save() {
		if (saveListener != null) {
			saveListener.save(memberMatches);
		}
	}

	private enum SourceType {
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

		public static SourceType getDefault() {
			return values()[0];
		}

		public JRadioButton newRadio(ActionListener listener, ButtonGroup group) {
			JRadioButton button = new JRadioButton(name(), this == getDefault());
			button.setActionCommand(name());
			button.addActionListener(listener);
			group.add(button);
			return button;
		}

		public abstract <T extends Entry> Collection<ClassEntry> getObfSourceClasses(MemberMatches<T> matches);
	}

	public interface SaveListener<T extends Entry> {
		void save(MemberMatches<T> matches);
	}
}
