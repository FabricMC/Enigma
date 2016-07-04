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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.convert.*;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsChecker;
import de.sciss.syntaxpane.DefaultSyntaxKit;


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
    private JFrame frame;
    private ClassSelector sourceClasses;
    private ClassSelector destClasses;
    private CodeReader sourceReader;
    private CodeReader destReader;
    private JLabel sourceClassLabel;
    private JLabel destClassLabel;
    private JButton matchButton;
    private Map<SourceType, JRadioButton> sourceTypeButtons;
    private JCheckBox advanceCheck;
    private JCheckBox top10Matches;

    private ClassMatches classMatches;
    private Deobfuscator sourceDeobfuscator;
    private Deobfuscator destDeobfuscator;
    private ClassEntry sourceClass;
    private ClassEntry destClass;
    private SourceType sourceType;
    private SaveListener saveListener;

    public ClassMatchingGui(ClassMatches matches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {

        this.classMatches = matches;
        this.sourceDeobfuscator = sourceDeobfuscator;
        this.destDeobfuscator = destDeobfuscator;

        // init frame
        this.frame = new JFrame(Constants.NAME + " - Class Matcher");
        final Container pane = this.frame.getContentPane();
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

        this.sourceClasses = new ClassSelector(ClassSelector.DEOBF_CLASS_COMPARATOR);
        this.sourceClasses.setListener(this::setSourceClass);
        JScrollPane sourceScroller = new JScrollPane(this.sourceClasses);
        sourcePanel.add(sourceScroller);

        // init dest side
        JPanel destPanel = new JPanel();
        destPanel.setLayout(new BoxLayout(destPanel, BoxLayout.PAGE_AXIS));
        destPanel.setPreferredSize(new Dimension(200, 0));
        pane.add(destPanel, BorderLayout.WEST);
        destPanel.add(new JLabel("Destination Classes"));

        this.top10Matches = new JCheckBox("Show only top 10 matches");
        destPanel.add(this.top10Matches);
        this.top10Matches.addActionListener(event -> toggleTop10Matches());

        this.destClasses = new ClassSelector(ClassSelector.DEOBF_CLASS_COMPARATOR);
        this.destClasses.setListener(this::setDestClass);
        JScrollPane destScroller = new JScrollPane(this.destClasses);
        destPanel.add(destScroller);

        JButton autoMatchButton = new JButton("AutoMatch");
        autoMatchButton.addActionListener(event -> autoMatch());
        destPanel.add(autoMatchButton);

        // init source panels
        DefaultSyntaxKit.initKit();
        this.sourceReader = new CodeReader();
        this.destReader = new CodeReader();

        // init all the splits
        JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, sourcePanel, new JScrollPane(this.sourceReader));
        splitLeft.setResizeWeight(0); // let the right side take all the slack
        JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(this.destReader), destPanel);
        splitRight.setResizeWeight(1); // let the left side take all the slack
        JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, splitRight);
        splitCenter.setResizeWeight(0.5); // resize 50:50
        pane.add(splitCenter, BorderLayout.CENTER);
        splitCenter.resetToPreferredSizes();

        // init bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());

        this.sourceClassLabel = new JLabel();
        this.sourceClassLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.destClassLabel = new JLabel();
        this.destClassLabel.setHorizontalAlignment(SwingConstants.LEFT);

        this.matchButton = new JButton();

        this.advanceCheck = new JCheckBox("Advance to next likely match");
        this.advanceCheck.addActionListener(event -> {
            if (this.advanceCheck.isSelected()) {
                advance();
            }
        });

        bottomPanel.add(this.sourceClassLabel);
        bottomPanel.add(this.matchButton);
        bottomPanel.add(this.destClassLabel);
        bottomPanel.add(this.advanceCheck);
        pane.add(bottomPanel, BorderLayout.SOUTH);

        // show the frame
        pane.doLayout();
        this.frame.setSize(1024, 576);
        this.frame.setMinimumSize(new Dimension(640, 480));
        this.frame.setVisible(true);
        this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // init state
        updateDestMappings();
        setSourceType(SourceType.getDefault());
        updateMatchButton();
        this.saveListener = null;
    }

    public void setSaveListener(SaveListener val) {
        this.saveListener = val;
    }

    private void updateDestMappings() {

        Mappings newMappings = MappingsConverter.newMappings(this.classMatches, this.sourceDeobfuscator.getMappings(), this.sourceDeobfuscator, this.destDeobfuscator);

        // look for dropped mappings
        MappingsChecker checker = new MappingsChecker(this.destDeobfuscator.getJarIndex());
        checker.dropBrokenMappings(newMappings);

        // count them
        int numDroppedFields = checker.getDroppedFieldMappings().size();
        int numDroppedMethods = checker.getDroppedMethodMappings().size();
        System.out.println(String.format("%d mappings from matched classes don't match the dest jar:\n\t%5d fields\n\t%5d methods",
                numDroppedFields + numDroppedMethods,
                numDroppedFields,
                numDroppedMethods
        ));

        this.destDeobfuscator.setMappings(newMappings);
    }

    protected void setSourceType(SourceType val) {

        // show the source classes
        this.sourceType = val;
        this.sourceClasses.setClasses(deobfuscateClasses(this.sourceType.getSourceClasses(this.classMatches), this.sourceDeobfuscator));

        // update counts
        for (SourceType sourceType : SourceType.values()) {
            this.sourceTypeButtons.get(sourceType).setText(String.format("%s (%d)",
                    sourceType.name(),
                    sourceType.getSourceClasses(this.classMatches).size()
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
        if (this.advanceCheck.isSelected()) {
            onGetDestClasses = this::pickBestDestClass;
        }

        setSourceClass(classEntry, onGetDestClasses);
    }

    protected void setSourceClass(ClassEntry classEntry, final Runnable onGetDestClasses) {

        // update the current source class
        this.sourceClass = classEntry;
        this.sourceClassLabel.setText(this.sourceClass != null ? this.sourceClass.getName() : "");

        if (this.sourceClass != null) {

            // show the dest class(es)
            ClassMatch match = this.classMatches.getMatchBySource(this.sourceDeobfuscator.obfuscateEntry(this.sourceClass));
            assert (match != null);
            if (match.destClasses.isEmpty()) {

                this.destClasses.setClasses(null);

                // run in a separate thread to keep ui responsive
                new Thread() {
                    @Override
                    public void run() {
                        destClasses.setClasses(deobfuscateClasses(getLikelyMatches(sourceClass), destDeobfuscator));
                        destClasses.expandAll();

                        if (onGetDestClasses != null) {
                            onGetDestClasses.run();
                        }
                    }
                }.start();

            } else {

                this.destClasses.setClasses(deobfuscateClasses(match.destClasses, this.destDeobfuscator));
                this.destClasses.expandAll();

                if (onGetDestClasses != null) {
                    onGetDestClasses.run();
                }
            }
        }

        setDestClass(null);
        this.sourceReader.decompileClass(this.sourceClass, this.sourceDeobfuscator, () -> this.sourceReader.navigateToClassDeclaration(this.sourceClass));

        updateMatchButton();
    }

    private Collection<ClassEntry> getLikelyMatches(ClassEntry sourceClass) {

        ClassEntry obfSourceClass = this.sourceDeobfuscator.obfuscateEntry(sourceClass);

        // set up identifiers
        ClassNamer namer = new ClassNamer(this.classMatches.getUniqueMatches());
        ClassIdentifier sourceIdentifier = new ClassIdentifier(this.sourceDeobfuscator.getJar(), this.sourceDeobfuscator.getJarIndex(), namer.getSourceNamer(), true);
        ClassIdentifier destIdentifier = new ClassIdentifier(this.destDeobfuscator.getJar(), this.destDeobfuscator.getJarIndex(), namer.getDestNamer(), true);

        try {

            // rank all the unmatched dest classes against the source class
            ClassIdentity sourceIdentity = sourceIdentifier.identify(obfSourceClass);
            List<ClassEntry> scoredDestClasses = Lists.newArrayList();
            for (ClassEntry unmatchedDestClass : this.classMatches.getUnmatchedDestClasses()) {
                ClassIdentity destIdentity = destIdentifier.identify(unmatchedDestClass);
                float score = 100.0f * (sourceIdentity.getMatchScore(destIdentity) + destIdentity.getMatchScore(sourceIdentity))
                        / (sourceIdentity.getMaxMatchScore() + destIdentity.getMaxMatchScore());
                scoredDestClasses.add(new ScoredClassEntry(unmatchedDestClass, score));
            }

            if (this.top10Matches.isSelected() && scoredDestClasses.size() > 10) {
                Collections.sort(scoredDestClasses, (a, b) -> {
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
        this.destClass = classEntry;
        this.destClassLabel.setText(this.destClass != null ? this.destClass.getName() : "");

        this.destReader.decompileClass(this.destClass, this.destDeobfuscator, () -> this.destReader.navigateToClassDeclaration(this.destClass));

        updateMatchButton();
    }

    private void updateMatchButton() {

        ClassEntry obfSource = this.sourceDeobfuscator.obfuscateEntry(this.sourceClass);
        ClassEntry obfDest = this.destDeobfuscator.obfuscateEntry(this.destClass);

        BiMap<ClassEntry, ClassEntry> uniqueMatches = this.classMatches.getUniqueMatches();
        boolean twoSelected = this.sourceClass != null && this.destClass != null;
        boolean isMatched = uniqueMatches.containsKey(obfSource) && uniqueMatches.containsValue(obfDest);
        boolean canMatch = !uniqueMatches.containsKey(obfSource) && !uniqueMatches.containsValue(obfDest);

        GuiTricks.deactivateButton(this.matchButton);
        if (twoSelected) {
            if (isMatched) {
                GuiTricks.activateButton(this.matchButton, "Unmatch", event -> onUnmatchClick());
            } else if (canMatch) {
                GuiTricks.activateButton(this.matchButton, "Match", event -> onMatchClick());
            }
        }
    }

    private void onMatchClick() {
        // precondition: source and dest classes are set correctly

        ClassEntry obfSource = this.sourceDeobfuscator.obfuscateEntry(this.sourceClass);
        ClassEntry obfDest = this.destDeobfuscator.obfuscateEntry(this.destClass);

        // remove the classes from their match
        this.classMatches.removeSource(obfSource);
        this.classMatches.removeDest(obfDest);

        // add them as matched classes
        this.classMatches.add(new ClassMatch(obfSource, obfDest));

        ClassEntry nextClass = null;
        if (this.advanceCheck.isSelected()) {
            nextClass = this.sourceClasses.getNextClass(this.sourceClass);
        }

        save();
        updateMatches();

        if (nextClass != null) {
            advance(nextClass);
        }
    }

    private void onUnmatchClick() {
        // precondition: source and dest classes are set to a unique match

        ClassEntry obfSource = this.sourceDeobfuscator.obfuscateEntry(this.sourceClass);

        // remove the source to break the match, then add the source back as unmatched
        this.classMatches.removeSource(obfSource);
        this.classMatches.add(new ClassMatch(obfSource, null));

        save();
        updateMatches();
    }

    private void updateMatches() {
        updateDestMappings();
        setDestClass(null);
        this.destClasses.setClasses(null);
        updateMatchButton();

        // remember where we were in the source tree
        String packageName = this.sourceClasses.getSelectedPackage();

        setSourceType(this.sourceType);

        this.sourceClasses.expandPackage(packageName);
    }

    private void save() {
        if (this.saveListener != null) {
            this.saveListener.save(this.classMatches);
        }
    }

    private void autoMatch() {

        System.out.println("Automatching...");

        // compute a new matching
        ClassMatching matching = MappingsConverter.computeMatching(this.sourceDeobfuscator.getJar(), this.sourceDeobfuscator.getJarIndex(),
                this.destDeobfuscator.getJar(), this.destDeobfuscator.getJarIndex(), this.classMatches.getUniqueMatches());
        ClassMatches newMatches = new ClassMatches(matching.matches());
        System.out.println(String.format("Automatch found %d new matches", newMatches.getUniqueMatches().size() - this.classMatches.getUniqueMatches().size()));

        // update the current matches
        this.classMatches = newMatches;
        save();
        updateMatches();
    }

    private void advance() {
        advance(null);
    }

    private void advance(ClassEntry sourceClass) {

        // make sure we have a source class
        if (sourceClass == null) {
            sourceClass = this.sourceClasses.getSelectedClass();
            if (sourceClass != null) {
                sourceClass = this.sourceClasses.getNextClass(sourceClass);
            } else {
                sourceClass = this.sourceClasses.getFirstClass();
            }
        }

        // set the source class
        setSourceClass(sourceClass, this::pickBestDestClass);
        this.sourceClasses.setSelectionClass(sourceClass);
    }

    private void pickBestDestClass() {

        // then, pick the best dest class
        ClassEntry firstClass = null;
        ScoredClassEntry bestDestClass = null;
        for (ClassSelectorPackageNode packageNode : this.destClasses.packageNodes()) {
            for (ClassSelectorClassNode classNode : this.destClasses.classNodes(packageNode)) {
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
        this.destClasses.setSelectionClass(destClass);
    }

    private void toggleTop10Matches() {
        if (this.sourceClass != null) {
            this.destClasses.clearSelection();
            this.destClasses.setClasses(deobfuscateClasses(getLikelyMatches(this.sourceClass), this.destDeobfuscator));
            this.destClasses.expandAll();
        }
    }
}
