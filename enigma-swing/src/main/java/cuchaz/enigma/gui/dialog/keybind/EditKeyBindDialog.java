package cuchaz.enigma.gui.dialog.keybind;

import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class EditKeyBindDialog extends JDialog {
    private final List<CombinationPanel> combinationPanels = new ArrayList<>();
    private final List<KeyBind.Combination> combinations;
    private final KeyBind keyBind;
    private final JPanel combinationsPanel;

    public EditKeyBindDialog(Frame owner, KeyBind bind) {
        super(owner, I18n.translate("menu.file.configure_keybinds.edit.title"), true);
        this.keyBind = bind;
        this.combinations = new ArrayList<>(keyBind.combinations());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Add buttons
        JPanel buttonsPanel = new JPanel(new GridLayout(0, 2));
        JButton addButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.add"));
        addButton.addActionListener(e -> addCombination());
        addButton.addMouseListener(mouseListener());
        buttonsPanel.add(addButton);
        JButton clearButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.clear"));
        clearButton.addActionListener(e -> clearCombinations());
        clearButton.addMouseListener(mouseListener());
        buttonsPanel.add(clearButton);
        JButton resetButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.reset"));
        resetButton.addActionListener(e -> reset());
        resetButton.addMouseListener(mouseListener());
        buttonsPanel.add(resetButton);
        contentPane.add(buttonsPanel, BorderLayout.NORTH);

        // Add combinations panel
        combinationsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        combinationsPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10)));
        combinationsPanel.addMouseListener(mouseListener());
        for (KeyBind.Combination combination : keyBind.combinations()) {
            CombinationPanel combinationPanel = new CombinationPanel(this, combination);
            combinationPanel.addMouseListener(mouseListener());
            combinationPanels.add(combinationPanel);
            combinationsPanel.add(combinationPanel);
        }
        contentPane.add(combinationsPanel, BorderLayout.CENTER);

        // Add confirmation buttons
        Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
        JButton saveButton = new JButton(I18n.translate("menu.file.configure_keybinds.save"));
        saveButton.addActionListener(event -> save());
        buttonContainer.add(saveButton);
        JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
        cancelButton.addActionListener(event -> cancel());
        buttonContainer.add(cancelButton);
        contentPane.add(buttonContainer, BorderLayout.SOUTH);

        addMouseListener(mouseListener());

        pack();
        setLocationRelativeTo(owner);
    }

    private void save() {
        boolean modified = !combinations.equals(keyBind.combinations());
        for (CombinationPanel combinationPanel : combinationPanels) {
            if (combinationPanel.isModified() && combinationPanel.isCombinationValid()) {
                modified = true;
                KeyBind.Combination combination = combinationPanel.getResultCombination();
                if (isNewCombination(combinationPanel)) {
                    combinations.add(combination);
                } else {
                    int index = combinations.indexOf(combinationPanel.getOriginalCombination());
                    if (index >= 0) {
                        combinations.set(index, combination);
                    } else {
                        combinations.add(combination);
                    }
                }
            }
        }

        if (modified) {
            keyBind.combinations().clear();
            keyBind.combinations().addAll(combinations);
        }

        setVisible(false);
        dispose();
    }

    private void cancel() {
        setVisible(false);
        dispose();
    }

    // Stop editing when the user clicks
    private MouseAdapter mouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                stopEditing(null);
            }
        };
    }

    protected void removeCombination(CombinationPanel combinationPanel) {
        combinations.remove(combinationPanel.getOriginalCombination());
        combinationsPanel.remove(combinationPanel);
        combinationPanels.remove(combinationPanel);
        pack();
    }

    private void addCombination() {
        CombinationPanel combinationPanel = CombinationPanel.createEmpty(this);
        combinationPanel.addMouseListener(mouseListener());
        combinationsPanel.add(combinationPanel);
        combinationPanels.add(combinationPanel);
        pack();
    }

    private void clearCombinations() {
        for (CombinationPanel combinationPanel : combinationPanels) {
            combinations.remove(combinationPanel.getOriginalCombination());
            combinationsPanel.remove(combinationPanel);
        }
        combinationPanels.clear();
        pack();
    }

    private void reset() {
        combinations.clear();
        combinationPanels.clear();
        combinationsPanel.removeAll();

        KeyBinds.resetToDefault(keyBind);
        combinations.addAll(keyBind.combinations());
        for (KeyBind.Combination combination : combinations) {
            CombinationPanel combinationPanel = new CombinationPanel(this, combination);
            combinationPanel.addMouseListener(mouseListener());
            combinationPanels.add(combinationPanel);
            combinationsPanel.add(combinationPanel);
        }
        pack();
    }

    private boolean isNewCombination(CombinationPanel panel) {
        return panel.getOriginalCombination() != KeyBind.Combination.EMPTY;
    }

    // Stop editing all combination panels but the excluded one
    protected void stopEditing(CombinationPanel excluded) {
        for (CombinationPanel combinationPanel : combinationPanels) {
            if (combinationPanel == excluded) continue;
            combinationPanel.stopEditing();
        }
    }
}
