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

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.convert.*;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsChecker;
import cuchaz.enigma.throwables.MappingConflict;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class ClassMatchingGui {

    private enum SourceType {
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

    public interface SaveListener {
        void save(ClassMatches matches);
    }

    // controls
    private JFrame                        frame;
    private ClassSelector                 sourceClasses;
    private ClassSelector                 destClasses;
    private CodeReader                    sourceReader;
    private CodeReader                    destReader;
    private JLabel                        sourceClassLabel;
    private JLabel                        destClassLabel;
    private JButton                       matchButton;
    private Map<SourceType, JRadioButton> sourceTypeButtons;
    private JCheckBox                     advanceCheck;
    private JCheckBox                     top10Matches;

    private ClassMatches classMatches;
    private Deobfuscator sourceDeobfuscator;
    private Deobfuscator destDeobfuscator;
    private ClassEntry   sourceClass;
    private ClassEntry   destClass;
    private SourceType   sourceType;
    private SaveListener saveListener;

    public ClassMatchingGui(ClassMatches matches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {

        classMatches = matches;
        this.sourceDeobfuscator = sourceDeobfuscator;
        this.destDeobfuscator = destDeobfuscator;

        // init frame
        frame = new JFrame(Constants.NAME + " - Class Matcher");
        final Container pane = frame.getContentPane();
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
        sourcePanel.add(sourceScroller);

        // init dest side
        JPanel destPanel = new JPanel();
        destPanel.setLayout(new BoxLayout(destPanel, BoxLayout.PAGE_AXIS));
        destPanel.setPreferredSize(new Dimension(200, 0));
        pane.add(destPanel, BorderLayout.WEST);
        destPanel.add(new JLabel("Destination Classes"));

        top10Matches = new JCheckBox("Show only top 10 matches");
        destPanel.add(top10Matches);
        top10Matches.addActionListener(event -> toggleTop10Matches());

        destClasses = new ClassSelector(null, ClassSelector.DEOBF_CLASS_COMPARATOR, false);
        destClasses.setSelectionListener(this::setDestClass);
        JScrollPane destScroller = new JScrollPane(destClasses);
        destPanel.add(destScroller);

        JButton autoMatchButton = new JButton("AutoMatch");
        autoMatchButton.addActionListener(event -> autoMatch());
        destPanel.add(autoMatchButton);

        // init source panels
        DefaultSyntaxKit.initKit();
        sourceReader = new CodeReader();
        destReader = new CodeReader();

        // init all the splits
        JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, sourcePanel, new JScrollPane(
                sourceReader));
        splitLeft.setResizeWeight(0); // let the right side take all the slack
        JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(destReader), destPanel);
        splitRight.setResizeWeight(1); // let the left side take all the slack
        JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, splitRight);
        splitCenter.setResizeWeight(0.5); // resize 50:50
        pane.add(splitCenter, BorderLayout.CENTER);
        splitCenter.resetToPreferredSizes();

        // init bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());

        sourceClassLabel = new JLabel();
        sourceClassLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        destClassLabel = new JLabel();
        destClassLabel.setHorizontalAlignment(SwingConstants.LEFT);

        matchButton = new JButton();

        advanceCheck = new JCheckBox("Advance to next likely match");
        advanceCheck.addActionListener(event -> {
            if (advanceCheck.isSelected()) {
                advance();
            }
        });

        bottomPanel.add(sourceClassLabel);
        bottomPanel.add(matchButton);
        bottomPanel.add(destClassLabel);
        bottomPanel.add(advanceCheck);
        pane.add(bottomPanel, BorderLayout.SOUTH);

        // show the frame
        pane.doLayout();
        frame.setSize(1024, 576);
        frame.setMinimumSize(new Dimension(640, 480));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // init state
        updateDestMappings();
        setSourceType(SourceType.getDefault());
        updateMatchButton();
        saveListener = null;
    }

    public void setSaveListener(SaveListener val) {
        saveListener = val;
    }

    private void updateDestMappings() {
        try {
            Mappings newMappings = MappingsConverter.newMappings(classMatches,
                    sourceDeobfuscator.getMappings(), sourceDeobfuscator, destDeobfuscator
            );

            // look for dropped mappings
            MappingsChecker checker = new MappingsChecker(destDeobfuscator.getJarIndex());
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

            destDeobfuscator.setMappings(newMappings);
        } catch (MappingConflict ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return;
        }
    }

    protected void setSourceType(SourceType val) {

        // show the source classes
        sourceType = val;
        sourceClasses.setClasses(deobfuscateClasses(sourceType.getSourceClasses(classMatches), sourceDeobfuscator));

        // update counts
        for (SourceType sourceType : SourceType.values()) {
            sourceTypeButtons.get(sourceType).setText(String.format("%s (%d)",
                    sourceType.name(),
                    sourceType.getSourceClasses(classMatches).size()
            ));
        }
    }

    private Collection<ClassEntry> deobfuscateClasses(Collection<ClassEntry> in, Deobfuscator deobfuscator) {
        List<ClassEntry> out = Lists.newArrayList();
        for (ClassEntry entry : in) {

            ClassEntry deobf = deobfuscator.deobfuscateEntry(entry);

            // make sure we preserve any scores
            if (entry instanceof ScoredClassEntry) {
                deobf = new ScoredClassEntry(deobf, ((ScoredClassEntry) entry).getScore());
            }

            out.add(deobf);
        }
        return out;
    }

    protected void setSourceClass(ClassEntry classEntry) {

        Runnable onGetDestClasses = null;
        if (advanceCheck.isSelected()) {
            onGetDestClasses = this::pickBestDestClass;
        }

        setSourceClass(classEntry, onGetDestClasses);
    }

    protected void setSourceClass(ClassEntry classEntry, final Runnable onGetDestClasses) {

        // update the current source class
        sourceClass = classEntry;
        sourceClassLabel.setText(sourceClass != null ? sourceClass.getName() : "");

        if (sourceClass != null) {

            // show the dest class(es)
            ClassMatch match = classMatches.getMatchBySource(sourceDeobfuscator.obfuscateEntry(sourceClass));
            assert (match != null);
            if (match.destClasses.isEmpty()) {

                destClasses.setClasses(null);

                // run in a separate thread to keep ui responsive
                new Thread(() ->
                {
                    destClasses.setClasses(deobfuscateClasses(getLikelyMatches(sourceClass), destDeobfuscator));
                    destClasses.expandAll();

                    if (onGetDestClasses != null) {
                        onGetDestClasses.run();
                    }
                }).start();

            } else {

                destClasses.setClasses(deobfuscateClasses(match.destClasses, destDeobfuscator));
                destClasses.expandAll();

                if (onGetDestClasses != null) {
                    onGetDestClasses.run();
                }
            }
        }

        setDestClass(null);
        sourceReader.decompileClass(
                sourceClass, sourceDeobfuscator, () -> sourceReader.navigateToClassDeclaration(sourceClass));

        updateMatchButton();
    }

    private Collection<ClassEntry> getLikelyMatches(ClassEntry sourceClass) {

        ClassEntry obfSourceClass = sourceDeobfuscator.obfuscateEntry(sourceClass);

        // set up identifiers
        ClassNamer namer = new ClassNamer(classMatches.getUniqueMatches());
        ClassIdentifier sourceIdentifier = new ClassIdentifier(
                sourceDeobfuscator.getJar(), sourceDeobfuscator.getJarIndex(),
                namer.getSourceNamer(), true
        );
        ClassIdentifier destIdentifier = new ClassIdentifier(
                destDeobfuscator.getJar(), destDeobfuscator.getJarIndex(),
                namer.getDestNamer(), true
        );

        try {

            // rank all the unmatched dest classes against the source class
            ClassIdentity sourceIdentity = sourceIdentifier.identify(obfSourceClass);
            List<ClassEntry> scoredDestClasses = Lists.newArrayList();
            for (ClassEntry unmatchedDestClass : classMatches.getUnmatchedDestClasses()) {
                ClassIdentity destIdentity = destIdentifier.identify(unmatchedDestClass);
                float score = 100.0f * (sourceIdentity.getMatchScore(destIdentity) + destIdentity.getMatchScore(sourceIdentity))
                        / (sourceIdentity.getMaxMatchScore() + destIdentity.getMaxMatchScore());
                scoredDestClasses.add(new ScoredClassEntry(unmatchedDestClass, score));
            }

            if (top10Matches.isSelected() && scoredDestClasses.size() > 10) {
                scoredDestClasses.sort((a, b) ->
                {
                    ScoredClassEntry sa = (ScoredClassEntry) a;
                    ScoredClassEntry sb = (ScoredClassEntry) b;
                    return -Float.compare(sa.getScore(), sb.getScore());
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
        destClass = classEntry;
        destClassLabel.setText(destClass != null ? destClass.getName() : "");

        destReader.decompileClass(destClass, destDeobfuscator, () -> destReader.navigateToClassDeclaration(destClass));

        updateMatchButton();
    }

    private void updateMatchButton() {

        ClassEntry obfSource = sourceDeobfuscator.obfuscateEntry(sourceClass);
        ClassEntry obfDest = destDeobfuscator.obfuscateEntry(destClass);

        BiMap<ClassEntry, ClassEntry> uniqueMatches = classMatches.getUniqueMatches();
        boolean twoSelected = sourceClass != null && destClass != null;
        boolean isMatched = uniqueMatches.containsKey(obfSource) && uniqueMatches.containsValue(obfDest);
        boolean canMatch = !uniqueMatches.containsKey(obfSource) && !uniqueMatches.containsValue(obfDest);

        GuiTricks.deactivateButton(matchButton);
        if (twoSelected) {
            if (isMatched) {
                GuiTricks.activateButton(matchButton, "Unmatch", event -> onUnmatchClick());
            } else if (canMatch) {
                GuiTricks.activateButton(matchButton, "Match", event -> onMatchClick());
            }
        }
    }

    private void onMatchClick() {
        // precondition: source and dest classes are set correctly

        ClassEntry obfSource = sourceDeobfuscator.obfuscateEntry(sourceClass);
        ClassEntry obfDest = destDeobfuscator.obfuscateEntry(destClass);

        // remove the classes from their match
        classMatches.removeSource(obfSource);
        classMatches.removeDest(obfDest);

        // add them as matched classes
        classMatches.add(new ClassMatch(obfSource, obfDest));

        ClassEntry nextClass = null;
        if (advanceCheck.isSelected()) {
            nextClass = sourceClasses.getNextClass(sourceClass);
        }

        save();
        updateMatches();

        if (nextClass != null) {
            advance(nextClass);
        }
    }

    private void onUnmatchClick() {
        // precondition: source and dest classes are set to a unique match

        ClassEntry obfSource = sourceDeobfuscator.obfuscateEntry(sourceClass);

        // remove the source to break the match, then add the source back as unmatched
        classMatches.removeSource(obfSource);
        classMatches.add(new ClassMatch(obfSource, null));

        save();
        updateMatches();
    }

    private void updateMatches() {
        updateDestMappings();
        setDestClass(null);
        destClasses.setClasses(null);
        updateMatchButton();

        // remember where we were in the source tree
        String packageName = sourceClasses.getSelectedPackage();

        setSourceType(sourceType);

        sourceClasses.expandPackage(packageName);
    }

    private void save() {
        if (saveListener != null) {
            saveListener.save(classMatches);
        }
    }

    private void autoMatch() {

        System.out.println("Automatching...");

        // compute a new matching
        ClassMatching matching = MappingsConverter.computeMatching(
                sourceDeobfuscator.getJar(), sourceDeobfuscator.getJarIndex(),
                destDeobfuscator.getJar(), destDeobfuscator.getJarIndex(),
                classMatches.getUniqueMatches()
        );
        ClassMatches newMatches = new ClassMatches(matching.matches());
        System.out.println(String.format("Automatch found %d new matches",
                newMatches.getUniqueMatches().size() - classMatches.getUniqueMatches().size()
        ));

        // update the current matches
        classMatches = newMatches;
        save();
        updateMatches();
    }

    private void advance() {
        advance(null);
    }

    private void advance(ClassEntry sourceClass) {

        // make sure we have a source class
        if (sourceClass == null) {
            sourceClass = sourceClasses.getSelectedClass();
            if (sourceClass != null) {
                sourceClass = sourceClasses.getNextClass(sourceClass);
            } else {
                sourceClass = sourceClasses.getFirstClass();
            }
        }

        // set the source class
        setSourceClass(sourceClass, this::pickBestDestClass);
        sourceClasses.setSelectionClass(sourceClass);
    }

    private void pickBestDestClass() {

        // then, pick the best dest class
        ClassEntry firstClass = null;
        ScoredClassEntry bestDestClass = null;
        for (ClassSelectorPackageNode packageNode : destClasses.packageNodes()) {
            for (ClassSelectorClassNode classNode : destClasses.classNodes(packageNode)) {
                if (firstClass == null) {
                    firstClass = classNode.getClassEntry();
                }
                if (classNode.getClassEntry() instanceof ScoredClassEntry) {
                    ScoredClassEntry scoredClass = (ScoredClassEntry) classNode.getClassEntry();
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
        destClasses.setSelectionClass(destClass);
    }

    private void toggleTop10Matches() {
        if (sourceClass != null) {
            destClasses.clearSelection();
            destClasses.setClasses(deobfuscateClasses(getLikelyMatches(sourceClass), destDeobfuscator));
            destClasses.expandAll();
        }
    }
}
