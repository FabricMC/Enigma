package cuchaz.enigma.gui.dialog.keybind;

import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class CombinationPanel extends JPanel {
    private static final List<Integer> MODIFIER_KEYS = List.of(KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_META);
    private static final List<Integer> MODIFIER_FLAGS = List.of(InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.ALT_DOWN_MASK, InputEvent.META_DOWN_MASK);
    private static final Color EDITING_BUTTON_FOREGROUND = Color.ORANGE;
    private final EditKeyBindDialog parent;
    private final JButton button;
    private final Color defaultButtonFg;
    private final KeyBind.Combination originalCombination;
    private final MutableCombination editingCombination;
    private final MutableCombination lastCombination;
    private boolean editing = false;

    public CombinationPanel(EditKeyBindDialog parent, KeyBind.Combination combination) {
        this.parent = parent;
        this.originalCombination = combination;
        this.editingCombination = MutableCombination.fromCombination(combination);
        this.lastCombination = editingCombination.copy();

        setLayout(new FlowLayout(FlowLayout.RIGHT));
        setBorder(new EmptyBorder(0, ScaleUtil.scale(15), 0, ScaleUtil.scale(15)));

        JButton removeButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.remove"));
        removeButton.addActionListener(e -> this.parent.removeCombination(this));
        removeButton.addMouseListener(mouseListener());
        add(removeButton);

        button = new JButton(getButtonText());
        defaultButtonFg = button.getForeground();
        button.addActionListener(e -> onButtonPressed());
        button.addMouseListener(mouseListener());
        button.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKeyPressed(e);
            }
        });
        add(button);
    }

    private String getButtonText() {
        return editingCombination.toString();
    }

    private void onButtonPressed() {
        if (editing) {
            stopEditing();
        } else {
            startEditing();
        }
    }

    protected void stopEditing() {
        if (editing) {
            editing = false;
            button.setForeground(defaultButtonFg);

            if (!editingCombination.isEmpty() && !editingCombination.isValid()) {
                // Reset combination to last one if invalid
                editingCombination.setFrom(lastCombination);
                update();
            } else {
                lastCombination.setFrom(editingCombination);
            }
        }
    }

    private void startEditing() {
        if (!editing) {
            editing = true;
            button.setForeground(EDITING_BUTTON_FOREGROUND);
        }
    }

    private void update() {
        button.setText(getButtonText());
        parent.pack();
    }

    private void onKeyPressed(KeyEvent e) {
        if (editing) {
            if (MODIFIER_KEYS.contains(e.getKeyCode())) {
                int modifierIndex = MODIFIER_KEYS.indexOf(e.getKeyCode());
                int modifier = MODIFIER_FLAGS.get(modifierIndex);
                editingCombination.setKeyModifiers(editingCombination.keyModifiers | modifier);
            } else {
                editingCombination.setKeyCode(e.getKeyCode());
            }
            update();
        }
    }

    // Stop editing other CombinationPanels when clicking on this panel
    private MouseAdapter mouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                parent.stopEditing(CombinationPanel.this);
            }
        };
    }

    public boolean isModified() {
        return !editingCombination.isSameCombination(originalCombination);
    }

    public boolean isCombinationValid() {
        return editingCombination.isValid();
    }

    public KeyBind.Combination getOriginalCombination() {
        return originalCombination;
    }

    public KeyBind.Combination getResultCombination() {
        return new KeyBind.Combination(editingCombination.keyCode, editingCombination.keyModifiers);
    }

    public static CombinationPanel createEmpty(EditKeyBindDialog parent) {
        return new CombinationPanel(parent, KeyBind.Combination.EMPTY);
    }

    private static class MutableCombination {
        private int keyCode;
        private int keyModifiers;

        private MutableCombination(int keyCode, int keyModifiers) {
            this.keyCode = keyCode;
            this.keyModifiers = keyModifiers;
        }

        public static MutableCombination fromCombination(KeyBind.Combination combination) {
            return new MutableCombination(combination.keyCode(), combination.keyModifiers());
        }

        public void setFrom(MutableCombination combination) {
            set(combination.getKeyCode(), combination.getKeyModifiers());
        }

        public void set(int keyCode, int keyModifiers) {
            this.keyCode = keyCode;
            this.keyModifiers = keyModifiers;
        }

        public int getKeyCode() {
            return this.keyCode;
        }

        public int getKeyModifiers() {
            return this.keyModifiers;
        }

        public void setKeyCode(int keyCode) {
            this.keyCode = keyCode;
        }

        public void setKeyModifiers(int keyModifiers) {
            this.keyModifiers = keyModifiers;
        }

        public MutableCombination copy() {
            return new MutableCombination(keyCode, keyModifiers);
        }

        @Override
        public String toString() {
            String modifiers = modifiersToString();
            String key = keyToString();
            if (!modifiers.isEmpty()) {
                return modifiers + "+" + key;
            }
            return key;
        }

        private String modifiersToString() {
            if (keyModifiers == 0) {
                return "";
            }
            return InputEvent.getModifiersExText(keyModifiers);
        }

        private String keyToString() {
            if (keyCode == -1) {
                return I18n.translate("menu.file.configure_keybinds.edit.empty");
            }
            return KeyEvent.getKeyText(keyCode);
        }

        public boolean isEmpty() {
            return keyCode == -1 && keyModifiers == 0;
        }

        public boolean isValid() {
            return keyCode != -1;
        }

        public boolean isSameCombination(Object obj) {
            if (obj instanceof KeyBind.Combination combination) {
                return combination.keyCode() == keyCode && combination.keyModifiers() == keyModifiers;
            } else if (obj instanceof MutableCombination mutableCombination) {
                return mutableCombination.keyCode == keyCode && mutableCombination.keyModifiers == keyModifiers;
            }
            return false;
        }
    }
}
