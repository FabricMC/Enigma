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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.convert.ClassIdentifier;
import cuchaz.enigma.convert.ClassIdentity;
import cuchaz.enigma.convert.ClassMatch;
import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.ClassMatching;
import cuchaz.enigma.convert.ClassNamer;
import cuchaz.enigma.convert.MappingsConverter;
import cuchaz.enigma.gui.ClassSelector.ClassSelectionListener;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsChecker;
import de.sciss.syntaxpane.DefaultSyntaxKit;


public class ClassMatchingGui {
	
	private static enum SourceType {
		Matched {
			
			@Override
			public Collection<ClassEntry> getSourceClasses(ClassMatches matches) {
				return matches.getUniqueMatches().keySet();
			}
		},
		Unmatched {
			
			@Override
			public Collection<ClassEntry> getSourceClasses(ClassMatches matches) {
				return matches.getUnmatchedSourceClasses();
			}
		},
		Ambiguous {
			
			@Override
			public Collection<ClassEntry> getSourceClasses(ClassMatches matches) {
				return matches.getAmbiguouslyMatchedSourceClasses();
			}
		};
		
		public JRadioButton newRadio(ActionListener listener, ButtonGroup group) {
			JRadioButton button = new JRadioButton(name(), this == getDefault());
			button.setActionCommand(name());
			button.addActionListener(listener);
			group.add(button);
			return button;
		}
		
		public abstract Collection<ClassEntry> getSourceClasses(ClassMatches matches);
		
		public static SourceType getDefault() {
			return values()[0];
		}
	}
	
	public static interface SaveListener {
		public void save(ClassMatches matches);
	}
	
	// controls
	private JFrame m_frame;
	private ClassSelector m_sourceClasses;
	private ClassSelector m_destClasses;
	private CodeReader m_sourceReader;
	private CodeReader m_destReader;
	private JLabel m_sourceClassLabel;
	private JLabel m_destClassLabel;
	private JButton m_matchButton;
	private Map<SourceType,JRadioButton> m_sourceTypeButtons;
	private JCheckBox m_advanceCheck;
	private JCheckBox m_top10Matches;
	
	private ClassMatches m_classMatches;
	private Deobfuscator m_sourceDeobfuscator;
	private Deobfuscator m_destDeobfuscator;
	private ClassEntry m_sourceClass;
	private ClassEntry m_destClass;
	private SourceType m_sourceType;
	private SaveListener m_saveListener;

	public ClassMatchingGui(ClassMatches matches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		
		m_classMatches = matches;
		m_sourceDeobfuscator = sourceDeobfuscator;
		m_destDeobfuscator = destDeobfuscator;
		
		// init frame
		m_frame = new JFrame(Constants.Name + " - Class Matcher");
		final Container pane = m_frame.getContentPane();
		pane.setLayout(new BorderLayout());
		
		// init source side
		JPanel sourcePanel = new JPanel();
		sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.PAGE_AXIS));
		sourcePanel.setPreferredSize(new Dimension(200, 0));
		pane.add(sourcePanel, BorderLayout.WEST);
		sourcePanel.add(new JLabel("Source Classes"));
		
		// init source type radios
		JPanel sourceTypePanel = new JPanel();
		sourcePanel.add(sourceTypePanel);
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
		sourcePanel.add(sourceScroller);
		
		// init dest side
		JPanel destPanel = new JPanel();
		destPanel.setLayout(new BoxLayout(destPanel, BoxLayout.PAGE_AXIS));
		destPanel.setPreferredSize(new Dimension(200, 0));
		pane.add(destPanel, BorderLayout.WEST);
		destPanel.add(new JLabel("Destination Classes"));
		
		m_top10Matches = new JCheckBox("Show only top 10 matches");
		destPanel.add(m_top10Matches);
		m_top10Matches.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				toggleTop10Matches();
			}
		});
		
		m_destClasses = new ClassSelector(ClassSelector.DeobfuscatedClassEntryComparator);
		m_destClasses.setListener(new ClassSelectionListener() {
			@Override
			public void onSelectClass(ClassEntry classEntry) {
				setDestClass(classEntry);
			}
		});
		JScrollPane destScroller = new JScrollPane(m_destClasses);
		destPanel.add(destScroller);
		
		JButton autoMatchButton = new JButton("AutoMatch");
		autoMatchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				autoMatch();
			}
		});
		destPanel.add(autoMatchButton);
		
		// init source panels
		DefaultSyntaxKit.initKit();
		m_sourceReader = new CodeReader();
		m_destReader = new CodeReader();
		
		// init all the splits
		JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, sourcePanel, new JScrollPane(m_sourceReader));
		splitLeft.setResizeWeight(0); // let the right side take all the slack
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(m_destReader), destPanel);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, splitRight);
		splitCenter.setResizeWeight(0.5); // resize 50:50
		pane.add(splitCenter, BorderLayout.CENTER);
		splitCenter.resetToPreferredSizes();
		
		// init bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());
		
		m_sourceClassLabel = new JLabel();
		m_sourceClassLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		m_destClassLabel = new JLabel();
		m_destClassLabel.setHorizontalAlignment(SwingConstants.LEFT);
		
		m_matchButton = new JButton();
		
		m_advanceCheck = new JCheckBox("Advance to next likely match");
		m_advanceCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (m_advanceCheck.isSelected()) {
					advance();
				}
			}
		});
		
		bottomPanel.add(m_sourceClassLabel);
		bottomPanel.add(m_matchButton);
		bottomPanel.add(m_destClassLabel);
		bottomPanel.add(m_advanceCheck);
		pane.add(bottomPanel, BorderLayout.SOUTH);
		
		// show the frame
		pane.doLayout();
		m_frame.setSize(1024, 576);
		m_frame.setMinimumSize(new Dimension(640, 480));
		m_frame.setVisible(true);
		m_frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		// init state
		updateDestMappings();
		setSourceType(SourceType.getDefault());
		updateMatchButton();
		m_saveListener = null;
	}
	
	public void setSaveListener(SaveListener val) {
		m_saveListener = val;
	}

	private void updateDestMappings() {
		
		Mappings newMappings = MappingsConverter.newMappings(
			m_classMatches,
			m_sourceDeobfuscator.getMappings(),
			m_sourceDeobfuscator,
			m_destDeobfuscator
		);
		
		// look for dropped mappings
		MappingsChecker checker = new MappingsChecker(m_destDeobfuscator.getJarIndex());
		checker.dropBrokenMappings(newMappings);
		
		// count them
		int numDroppedFields = checker.getDroppedFieldMappings().size();
		int numDroppedMethods = checker.getDroppedMethodMappings().size();
		System.out.println(String.format(
			"%d mappings from matched classes don't match the dest jar:\n\t%5d fields\n\t%5d methods",
			numDroppedFields + numDroppedMethods,
			numDroppedFields,
			numDroppedMethods
		));
		
		m_destDeobfuscator.setMappings(newMappings);
	}

	protected void setSourceType(SourceType val) {
		
		// show the source classes
		m_sourceType = val;
		m_sourceClasses.setClasses(deobfuscateClasses(m_sourceType.getSourceClasses(m_classMatches), m_sourceDeobfuscator));
		
		// update counts
		for (SourceType sourceType : SourceType.values()) {
			m_sourceTypeButtons.get(sourceType).setText(String.format("%s (%d)",
				sourceType.name(),
				sourceType.getSourceClasses(m_classMatches).size()
			));
		}
	}
	
	private Collection<ClassEntry> deobfuscateClasses(Collection<ClassEntry> in, Deobfuscator deobfuscator) {
		List<ClassEntry> out = Lists.newArrayList();
		for (ClassEntry entry : in) {
			
			ClassEntry deobf = deobfuscator.deobfuscateEntry(entry);
			
			// make sure we preserve any scores
			if (entry instanceof ScoredClassEntry) {
				deobf = new ScoredClassEntry(deobf, ((ScoredClassEntry)entry).getScore());
			}
			
			out.add(deobf);
		}
		return out;
	}

	protected void setSourceClass(ClassEntry classEntry) {
		
		Runnable onGetDestClasses = null;
		if (m_advanceCheck.isSelected()) {
			onGetDestClasses = new Runnable() {
				@Override
				public void run() {
					pickBestDestClass();
				}
			};
		}
		
		setSourceClass(classEntry, onGetDestClasses);
	}
	
	protected void setSourceClass(ClassEntry classEntry, final Runnable onGetDestClasses) {

		// update the current source class
		m_sourceClass = classEntry;
		m_sourceClassLabel.setText(m_sourceClass != null ? m_sourceClass.getName() : "");
		
		if (m_sourceClass != null) {
			
			// show the dest class(es)
			ClassMatch match = m_classMatches.getMatchBySource(m_sourceDeobfuscator.obfuscateEntry(m_sourceClass));
			assert(match != null);
			if (match.destClasses.isEmpty()) {
				
				m_destClasses.setClasses(null);
				
				// run in a separate thread to keep ui responsive
				new Thread() {
					@Override
					public void run() {
						m_destClasses.setClasses(deobfuscateClasses(getLikelyMatches(m_sourceClass), m_destDeobfuscator));
						m_destClasses.expandAll();
						
						if (onGetDestClasses != null) {
							onGetDestClasses.run();
						}
					}
				}.start();
				
			} else {
				
				m_destClasses.setClasses(deobfuscateClasses(match.destClasses, m_destDeobfuscator));
				m_destClasses.expandAll();
				
				if (onGetDestClasses != null) {
					onGetDestClasses.run();
				}
			}
		}
		
		setDestClass(null);
		m_sourceReader.decompileClass(m_sourceClass, m_sourceDeobfuscator, new Runnable() {
			@Override
			public void run() {
				m_sourceReader.navigateToClassDeclaration(m_sourceClass);
			}
		});
		
		updateMatchButton();
	}

	private Collection<ClassEntry> getLikelyMatches(ClassEntry sourceClass) {
		
		ClassEntry obfSourceClass = m_sourceDeobfuscator.obfuscateEntry(sourceClass);
		
		// set up identifiers
		ClassNamer namer = new ClassNamer(m_classMatches.getUniqueMatches());
		ClassIdentifier sourceIdentifier = new ClassIdentifier(
			m_sourceDeobfuscator.getJar(), m_sourceDeobfuscator.getJarIndex(),
			namer.getSourceNamer(), true
		);
		ClassIdentifier destIdentifier = new ClassIdentifier(
			m_destDeobfuscator.getJar(), m_destDeobfuscator.getJarIndex(),
			namer.getDestNamer(), true
		);
		
		try {
			
			// rank all the unmatched dest classes against the source class
			ClassIdentity sourceIdentity = sourceIdentifier.identify(obfSourceClass);
			List<ClassEntry> scoredDestClasses = Lists.newArrayList();
			for (ClassEntry unmatchedDestClass : m_classMatches.getUnmatchedDestClasses()) {
				ClassIdentity destIdentity = destIdentifier.identify(unmatchedDestClass);
				float score = 100.0f*(sourceIdentity.getMatchScore(destIdentity) + destIdentity.getMatchScore(sourceIdentity))
					/(sourceIdentity.getMaxMatchScore() + destIdentity.getMaxMatchScore());
				scoredDestClasses.add(new ScoredClassEntry(unmatchedDestClass, score));
			}
			
			if (m_top10Matches.isSelected() && scoredDestClasses.size() > 10) {
				Collections.sort(scoredDestClasses, new Comparator<ClassEntry>() {
					@Override
					public int compare(ClassEntry a, ClassEntry b) {
						ScoredClassEntry sa = (ScoredClassEntry)a;
						ScoredClassEntry sb = (ScoredClassEntry)b;
						return -Float.compare(sa.getScore(), sb.getScore());
					}
				});
				scoredDestClasses = scoredDestClasses.subList(0, 10);
			}
			
			return scoredDestClasses;
			
		} catch (ClassNotFoundException ex) {
			throw new Error("Unable to find class " + ex.getMessage());
		}
	}
	
	protected void setDestClass(ClassEntry classEntry) {
		
		// update the current source class
		m_destClass = classEntry;
		m_destClassLabel.setText(m_destClass != null ? m_destClass.getName() : "");
		
		m_destReader.decompileClass(m_destClass, m_destDeobfuscator, new Runnable() {
			@Override
			public void run() {
				m_destReader.navigateToClassDeclaration(m_destClass);
			}
		});
		
		updateMatchButton();
	}
	
	private void updateMatchButton() {
		
		ClassEntry obfSource = m_sourceDeobfuscator.obfuscateEntry(m_sourceClass);
		ClassEntry obfDest = m_destDeobfuscator.obfuscateEntry(m_destClass);
		
		BiMap<ClassEntry,ClassEntry> uniqueMatches = m_classMatches.getUniqueMatches();
		boolean twoSelected = m_sourceClass != null && m_destClass != null;
		boolean isMatched = uniqueMatches.containsKey(obfSource) && uniqueMatches.containsValue(obfDest);
		boolean canMatch = !uniqueMatches.containsKey(obfSource) && ! uniqueMatches.containsValue(obfDest);
		
		GuiTricks.deactivateButton(m_matchButton);
		if (twoSelected) {
			if (isMatched) {
				GuiTricks.activateButton(m_matchButton, "Unmatch", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						onUnmatchClick();
					}
				});
			} else if (canMatch) {
				GuiTricks.activateButton(m_matchButton, "Match", new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						onMatchClick();
					}
				});
			}
		}
	}

	private void onMatchClick() {
		// precondition: source and dest classes are set correctly
		
		ClassEntry obfSource = m_sourceDeobfuscator.obfuscateEntry(m_sourceClass);
		ClassEntry obfDest = m_destDeobfuscator.obfuscateEntry(m_destClass);
		
		// remove the classes from their match
		m_classMatches.removeSource(obfSource);
		m_classMatches.removeDest(obfDest);
		
		// add them as matched classes
		m_classMatches.add(new ClassMatch(obfSource, obfDest));
		
		ClassEntry nextClass = null;
		if (m_advanceCheck.isSelected()) {
			nextClass = m_sourceClasses.getNextClass(m_sourceClass);
		}
		
		save();
		updateMatches();
		
		if (nextClass != null) {
			advance(nextClass);
		}
	}
	
	private void onUnmatchClick() {
		// precondition: source and dest classes are set to a unique match
		
		ClassEntry obfSource = m_sourceDeobfuscator.obfuscateEntry(m_sourceClass);
		
		// remove the source to break the match, then add the source back as unmatched
		m_classMatches.removeSource(obfSource);
		m_classMatches.add(new ClassMatch(obfSource, null));
		
		save();
		updateMatches();
	}
	
	private void updateMatches() {
		updateDestMappings();
		setDestClass(null);
		m_destClasses.setClasses(null);
		updateMatchButton();
		
		// remember where we were in the source tree
		String packageName = m_sourceClasses.getSelectedPackage();
		
		setSourceType(m_sourceType);
		
		m_sourceClasses.expandPackage(packageName);
	}
	
	private void save() {
		if (m_saveListener != null) {
			m_saveListener.save(m_classMatches);
		}
	}
	
	private void autoMatch() {
		
		System.out.println("Automatching...");
		
		// compute a new matching
		ClassMatching matching = MappingsConverter.computeMatching(
			m_sourceDeobfuscator.getJar(), m_sourceDeobfuscator.getJarIndex(),
			m_destDeobfuscator.getJar(), m_destDeobfuscator.getJarIndex(),
			m_classMatches.getUniqueMatches()
		);
		ClassMatches newMatches = new ClassMatches(matching.matches());
		System.out.println(String.format("Automatch found %d new matches",
			newMatches.getUniqueMatches().size() - m_classMatches.getUniqueMatches().size()
		));
		
		// update the current matches
		m_classMatches = newMatches;
		save();
		updateMatches();
	}
	
	private void advance() {
		advance(null);
	}
	
	private void advance(ClassEntry sourceClass) {

		// make sure we have a source class
		if (sourceClass == null) {
			sourceClass = m_sourceClasses.getSelectedClass();
			if (sourceClass != null) {
				sourceClass = m_sourceClasses.getNextClass(sourceClass);
			} else {
				sourceClass = m_sourceClasses.getFirstClass();
			}
		}
		
		// set the source class
		setSourceClass(sourceClass, new Runnable() {
			@Override
			public void run() {
				pickBestDestClass();
			}
		});
		m_sourceClasses.setSelectionClass(sourceClass);
	}
	
	private void pickBestDestClass() {
		
		// then, pick the best dest class
		ClassEntry firstClass = null;
		ScoredClassEntry bestDestClass = null;
		for (ClassSelectorPackageNode packageNode : m_destClasses.packageNodes()) {
			for (ClassSelectorClassNode classNode : m_destClasses.classNodes(packageNode)) {
				if (firstClass == null) {
					firstClass = classNode.getClassEntry();
				}
				if (classNode.getClassEntry() instanceof ScoredClassEntry) {
					ScoredClassEntry scoredClass = (ScoredClassEntry)classNode.getClassEntry();
					if (bestDestClass == null || bestDestClass.getScore() < scoredClass.getScore()) {
						bestDestClass = scoredClass;
					}
				}
			}
		}
		
		// pick the entry to show
		ClassEntry destClass = null;
		if (bestDestClass != null) {
			destClass = bestDestClass;
		} else if (firstClass != null) {
			destClass = firstClass;
		}
		
		setDestClass(destClass);
		m_destClasses.setSelectionClass(destClass);
	}
	
	private void toggleTop10Matches() {
		if (m_sourceClass != null) {
			m_destClasses.clearSelection();
			m_destClasses.setClasses(deobfuscateClasses(getLikelyMatches(m_sourceClass), m_destDeobfuscator));
			m_destClasses.expandAll();
		}
	}
}
