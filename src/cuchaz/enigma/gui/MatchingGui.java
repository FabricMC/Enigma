package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.convert.ClassIdentifier;
import cuchaz.enigma.convert.ClassIdentity;
import cuchaz.enigma.convert.ClassMatch;
import cuchaz.enigma.convert.ClassNamer;
import cuchaz.enigma.convert.Matches;
import cuchaz.enigma.gui.ClassSelector.ClassSelectionListener;
import cuchaz.enigma.mapping.ClassEntry;
import de.sciss.syntaxpane.DefaultSyntaxKit;


public class MatchingGui {
	
	private static enum SourceType {
		Matched {
			
			@Override
			public Collection<ClassEntry> getSourceClasses(Matches matches) {
				return matches.getUniqueMatches().keySet();
			}
		},
		Unmatched {
			
			@Override
			public Collection<ClassEntry> getSourceClasses(Matches matches) {
				return matches.getUnmatchedSourceClasses();
			}
		},
		Ambiguous {
			
			@Override
			public Collection<ClassEntry> getSourceClasses(Matches matches) {
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
		
		public abstract Collection<ClassEntry> getSourceClasses(Matches matches);
		
		public static SourceType getDefault() {
			return values()[0];
		}
	}
	
	// controls
	private JFrame m_frame;
	private ClassSelector m_sourceClasses;
	private ClassSelector m_destClasses;
	private JEditorPane m_sourceReader;
	private JEditorPane m_destReader;
	private JLabel m_sourceClassLabel;
	private JLabel m_destClassLabel;
	private JButton m_matchButton;
	
	private Matches m_matches;
	private Deobfuscator m_sourceDeobfuscator;
	private Deobfuscator m_destDeobfuscator;
	private ClassEntry m_sourceClass;
	private ClassEntry m_destClass;

	public MatchingGui(Matches matches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		
		m_matches = matches;
		m_sourceDeobfuscator = sourceDeobfuscator;
		m_destDeobfuscator = destDeobfuscator;
		
		// init frame
		m_frame = new JFrame(Constants.Name);
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
		for (SourceType sourceType : SourceType.values()) {
			sourceTypePanel.add(sourceType.newRadio(sourceTypeListener, sourceTypeButtons));
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
		
		m_destClasses = new ClassSelector(ClassSelector.DeobfuscatedClassEntryComparator);
		m_destClasses.setListener(new ClassSelectionListener() {
			@Override
			public void onSelectClass(ClassEntry classEntry) {
				setDestClass(classEntry);
			}
		});
		JScrollPane destScroller = new JScrollPane(m_destClasses);
		destPanel.add(destScroller);
		
		// init source panels
		DefaultSyntaxKit.initKit();
		m_sourceReader = new JEditorPane();
		m_sourceReader.setEditable(false);
		m_sourceReader.setContentType("text/java");
		m_destReader = new JEditorPane();
		m_destReader.setEditable(false);
		m_destReader.setContentType("text/java");
		
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
		m_sourceClassLabel.setPreferredSize(new Dimension(300, 0));
		m_destClassLabel = new JLabel();
		m_destClassLabel.setPreferredSize(new Dimension(300, 0));
		
		m_matchButton = new JButton();
		m_matchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				onMatchClick();
			}
		});
		m_matchButton.setPreferredSize(new Dimension(140, 24));
		
		bottomPanel.add(m_sourceClassLabel);
		bottomPanel.add(m_matchButton);
		bottomPanel.add(m_destClassLabel);
		pane.add(bottomPanel, BorderLayout.SOUTH);
		
		// show the frame
		pane.doLayout();
		m_frame.setSize(1024, 576);
		m_frame.setMinimumSize(new Dimension(640, 480));
		m_frame.setVisible(true);
		m_frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		// init state
		setSourceType(SourceType.getDefault());
		updateMatchButton();
	}

	protected void setSourceType(SourceType val) {
		// show the source classes
		m_sourceClasses.setClasses(deobfuscateClasses(val.getSourceClasses(m_matches), m_sourceDeobfuscator));
	}
	
	private Collection<ClassEntry> deobfuscateClasses(Collection<ClassEntry> in, Deobfuscator deobfuscator) {
		List<ClassEntry> out = Lists.newArrayList();
		for (ClassEntry entry : in) {
			out.add(deobfuscator.deobfuscateEntry(entry));
		}
		return out;
	}

	protected void setSourceClass(ClassEntry classEntry) {
		
		// update the current source class
		m_sourceClass = classEntry;
		m_sourceClassLabel.setText(m_sourceClass != null ? m_sourceClass.getName() : "");
		
		if (m_sourceClass != null) {
			
			// show the dest class(es)
			ClassMatch match = m_matches.getMatchBySource(m_sourceDeobfuscator.obfuscateEntry(m_sourceClass));
			assert(match != null);
			if (match.destClasses.isEmpty()) {
				m_destClasses.setClasses(deobfuscateClasses(getLikelyMatches(m_sourceClass), m_destDeobfuscator));
			} else {
				m_destClasses.setClasses(deobfuscateClasses(match.destClasses, m_destDeobfuscator));
			}
			m_destClasses.expandRow(0);
		}
		
		setDestClass(null);
		readSource(m_sourceClass, m_sourceDeobfuscator, m_sourceReader);
		
		updateMatchButton();
	}

	private Collection<ClassEntry> getLikelyMatches(ClassEntry sourceClass) {
		
		ClassEntry obfSourceClass = m_sourceDeobfuscator.obfuscateEntry(sourceClass);
		
		// set up identifiers
		ClassNamer namer = new ClassNamer(m_matches.getUniqueMatches());
		ClassIdentifier sourceIdentifier = new ClassIdentifier(
			m_sourceDeobfuscator.getJar(), m_sourceDeobfuscator.getJarIndex(),
			namer.getSourceNamer(), true
		);
		ClassIdentifier destIdentifier = new ClassIdentifier(
			m_destDeobfuscator.getJar(), m_destDeobfuscator.getJarIndex(),
			namer.getDestNamer(), true
		);
		
		// rank all the unmatched dest classes against the source class
		ClassIdentity sourceIdentity = sourceIdentifier.identify(obfSourceClass);
		Multimap<Float,ClassEntry> scoredDestClasses = ArrayListMultimap.create();
		for (ClassEntry unmatchedDestClass : m_matches.getUnmatchedDestClasses()) {
			ClassIdentity destIdentity = destIdentifier.identify(unmatchedDestClass);
			float score = 100.0f*(sourceIdentity.getMatchScore(destIdentity) + destIdentity.getMatchScore(sourceIdentity))
				/(sourceIdentity.getMaxMatchScore() + destIdentity.getMaxMatchScore());
			scoredDestClasses.put(score, unmatchedDestClass);
		}
		
		// sort by scores
		List<Float> scores = new ArrayList<Float>(scoredDestClasses.keySet());
		Collections.sort(scores, Collections.reverseOrder());
		
		// collect the scored classes in order
		List<ClassEntry> scoredClasses = Lists.newArrayList();
		for (float score : scores) {
			for (ClassEntry classEntry : scoredDestClasses.get(score)) {
				scoredClasses.add(new DecoratedClassEntry(classEntry, String.format("%.0f%% ", score)));
				if (scoredClasses.size() > 10) {
					return scoredClasses;
				}
			}
		}
		return scoredClasses;
	}
	
	protected void setDestClass(ClassEntry classEntry) {
		
		// update the current source class
		m_destClass = classEntry;
		m_destClassLabel.setText(m_destClass != null ? m_destClass.getName() : "");
		
		readSource(m_destClass, m_destDeobfuscator, m_destReader);
		
		updateMatchButton();
	}

	protected void readSource(final ClassEntry classEntry, final Deobfuscator deobfuscator, final JEditorPane reader) {
		
		if (classEntry == null) {
			reader.setText(null);
			return;
		}
		
		reader.setText("(decompiling...)");

		// run decompiler in a separate thread to keep ui responsive
		new Thread() {
			@Override
			public void run() {
				
				// get the outermost class
				ClassEntry obfClassEntry = deobfuscator.obfuscateEntry(classEntry);
				List<ClassEntry> classChain = deobfuscator.getJarIndex().getObfClassChain(obfClassEntry);
				ClassEntry obfOutermostClassEntry = classChain.get(0);
				
				// decompile it
				reader.setText(deobfuscator.getSource(deobfuscator.getSourceTree(obfOutermostClassEntry.getName())));
			}
		}.start();
	}
	
	private void updateMatchButton() {
		
		boolean twoSelected = m_sourceClass != null && m_destClass != null;
		boolean isMatched = twoSelected && m_matches.getUniqueMatches().containsKey(m_sourceDeobfuscator.obfuscateEntry(m_sourceClass));
		
		m_matchButton.setEnabled(twoSelected);
		if (twoSelected) {
			if (isMatched) {
				m_matchButton.setText("Unmatch");
			} else {
				m_matchButton.setText("Match");
			}
		} else {
			m_matchButton.setText("");
		}
	}
	
	protected void onMatchClick() {
		// TODO
	}
	
	/*
	private static List<String> getClassNames(Collection<ClassEntry> classes) {
		List<String> out = Lists.newArrayList();
		for (ClassEntry c : classes) {
			out.add(c.getName());
		}
		Collections.sort(out);
		return out;
	}
	*/
}
