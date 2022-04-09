package cuchaz.enigma.gui.config.keybind;

import cuchaz.enigma.utils.I18n;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record KeyBind(String name, String category, List<Combination> combinations) {
    public record Combination(int keyCode, int keyModifiers) {
        public static final Combination EMPTY = new Combination(-1, 0);

        public boolean matches(KeyEvent e) {
            return e.getKeyCode() == keyCode && e.getModifiersEx() == keyModifiers;
        }

        public KeyStroke toKeyStroke(int modifiers) {
            modifiers = keyModifiers | modifiers;
            return KeyStroke.getKeyStroke(keyCode, modifiers);
        }

        public String serialize() {
            return keyCode + ";" + Integer.toString(keyModifiers, 16);
        }

        public static Combination deserialize(String str) {
            String[] parts = str.split(";", 2);
            return new Combination(Integer.parseInt(parts[0]), Integer.parseInt(parts[1], 16));
        }

        @Override
        public String toString() {
            return "Combination[keyCode=" + keyCode + ", keyModifiers=0x" + Integer.toString(keyModifiers, 16).toUpperCase(Locale.ROOT) + "]";
        }
    }

    public void setFrom(KeyBind other) {
        this.combinations.clear();
        this.combinations.addAll(other.combinations);
    }

    public boolean matches(KeyEvent e) {
        return combinations.stream().anyMatch(c -> c.matches(e));
    }

    public KeyStroke toKeyStroke(int modifiers) {
        return isEmpty() ? null : combinations.get(0).toKeyStroke(modifiers);
    }

    public KeyStroke toKeyStroke() {
        return toKeyStroke(0);
    }

    public int getKeyCode() {
        return isEmpty() ? -1 : combinations.get(0).keyCode;
    }

    public boolean isEmpty() {
        return combinations.isEmpty();
    }

    public String[] serializeCombinations() {
        return combinations.stream().map(Combination::serialize).toArray(String[]::new);
    }

    public void deserializeCombinations(String[] serialized) {
        combinations.clear();
        for (String serializedCombination : serialized) {
            if (!serializedCombination.isEmpty()) {
                combinations.add(Combination.deserialize(serializedCombination));
            } else {
                System.out.println("warning: empty combination deserialized for keybind " + (category.isEmpty() ? "" : category + ".") + name);
            }
        }
    }

    private String getTranslationKey() {
        return "keybind." + (category.isEmpty() ? "" : category + ".") + this.name;
    }

    public String getTranslatedName() {
        return I18n.translate(getTranslationKey());
    }

    public KeyBind copy() {
        return new KeyBind(name, category, new ArrayList<>(combinations));
    }

    public KeyBind toImmutable() {
        return new KeyBind(name, category, List.copyOf(combinations));
    }

    public boolean isSameKeyBind(KeyBind other) {
        return name.equals(other.name) && category.equals(other.category);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(String name, String category) {
        return new Builder(name, category);
    }

    public static class Builder {
        private final String name;
        private final String category;
        private final List<Combination> combinations = new ArrayList<>();
        private int modifiers = 0;

        private Builder(String name) {
            this.name = name;
            this.category = "";
        }

        private Builder(String name, String category) {
            this.name = name;
            this.category = category;
        }

        public KeyBind build() {
            return new KeyBind(name, category, combinations);
        }

        public Builder key(int keyCode, int keyModifiers) {
            combinations.add(new Combination(keyCode, keyModifiers | modifiers));
            return this;
        }

        public Builder key(int keyCode) {
            return key(keyCode, 0);
        }

        public Builder keys(int... keyCodes) {
            for (int keyCode : keyCodes) {
                key(keyCode);
            }
            return this;
        }

        public Builder mod(int modifiers) {
            this.modifiers |= modifiers;
            return this;
        }
    }
}
