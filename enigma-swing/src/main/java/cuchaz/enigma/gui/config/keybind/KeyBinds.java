package cuchaz.enigma.gui.config.keybind;

import cuchaz.enigma.gui.config.KeyBindsConfig;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class KeyBinds {
    private static final String QUICK_FIND_DIALOG_CATEGORY = "quick_find_dialog";
    private static final String SEARCH_DIALOG_CATEGORY = "search_dialog";
    private static final String EDITOR_CATEGORY = "editor";
    private static final String MENU_CATEGORY = "menu";

    public static final KeyBind EXIT = KeyBind.builder("close").key(KeyEvent.VK_ESCAPE).build();
    public static final KeyBind DIALOG_SAVE = KeyBind.builder("dialog_save").key(KeyEvent.VK_ENTER).build();

    public static final KeyBind QUICK_FIND_DIALOG_IGNORE_CASE = KeyBind.builder("ignore_case", QUICK_FIND_DIALOG_CATEGORY).key(KeyEvent.VK_C).build();
    public static final KeyBind QUICK_FIND_DIALOG_REGEX = KeyBind.builder("regex", QUICK_FIND_DIALOG_CATEGORY).key(KeyEvent.VK_R).build();
    public static final KeyBind QUICK_FIND_DIALOG_WRAP = KeyBind.builder("wrap", QUICK_FIND_DIALOG_CATEGORY).key(KeyEvent.VK_W).build();
    public static final KeyBind QUICK_FIND_DIALOG_NEXT = KeyBind.builder("next", QUICK_FIND_DIALOG_CATEGORY).key(KeyEvent.VK_ENTER).build();
    public static final KeyBind QUICK_FIND_DIALOG_PREVIOUS = KeyBind.builder("previous", QUICK_FIND_DIALOG_CATEGORY).mod(KeyEvent.SHIFT_DOWN_MASK).key(KeyEvent.VK_ENTER).build();
    public static final KeyBind SEARCH_DIALOG_NEXT = KeyBind.builder("next", SEARCH_DIALOG_CATEGORY).key(KeyEvent.VK_DOWN).build();
    public static final KeyBind SEARCH_DIALOG_PREVIOUS = KeyBind.builder("previous", SEARCH_DIALOG_CATEGORY).key(KeyEvent.VK_UP).build();

    public static final KeyBind EDITOR_RENAME = KeyBind.builder("rename", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_R).build();
    public static final KeyBind EDITOR_PASTE = KeyBind.builder("paste", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_V).build();
    public static final KeyBind EDITOR_EDIT_JAVADOC = KeyBind.builder("edit_javadoc", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_D).build();
    public static final KeyBind EDITOR_SHOW_INHERITANCE = KeyBind.builder("show_inheritance", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_I).build();
    public static final KeyBind EDITOR_SHOW_IMPLEMENTATIONS = KeyBind.builder("show_implementations", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_M).build();
    public static final KeyBind EDITOR_SHOW_CALLS = KeyBind.builder("show_calls", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_C).build();
    public static final KeyBind EDITOR_SHOW_CALLS_SPECIFIC = KeyBind.builder("show_calls_specific", EDITOR_CATEGORY).key(KeyEvent.VK_C).mod(KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).build();
    public static final KeyBind EDITOR_OPEN_ENTRY = KeyBind.builder("open_entry", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_N).build();
    public static final KeyBind EDITOR_OPEN_PREVIOUS = KeyBind.builder("open_previous", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_P).build();
    public static final KeyBind EDITOR_OPEN_NEXT = KeyBind.builder("open_next", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_E).build();
    public static final KeyBind EDITOR_TOGGLE_MAPPING = KeyBind.builder("toggle_mapping", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_O).build();
    public static final KeyBind EDITOR_ZOOM_IN = KeyBind.builder("zoom_in", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).keys(KeyEvent.VK_PLUS, KeyEvent.VK_ADD, KeyEvent.VK_EQUALS).build();
    public static final KeyBind EDITOR_ZOOM_OUT = KeyBind.builder("zoom_out", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).keys(KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT).build();
    public static final KeyBind EDITOR_CLOSE_TAB = KeyBind.builder("close_tab", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_4).build();
    public static final KeyBind EDITOR_RELOAD_CLASS = KeyBind.builder("reload_class", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_F5).build();
    public static final KeyBind EDITOR_QUICK_FIND = KeyBind.builder("quick_find", EDITOR_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_F).build();

    public static final KeyBind SAVE_MAPPINGS = KeyBind.builder("save", MENU_CATEGORY).mod(KeyEvent.CTRL_DOWN_MASK).key(KeyEvent.VK_S).build();
    public static final KeyBind DROP_MAPPINGS = KeyBind.builder("drop_mappings", MENU_CATEGORY).build();
    public static final KeyBind RELOAD_MAPPINGS = KeyBind.builder("reload_mappings", MENU_CATEGORY).build();
    public static final KeyBind RELOAD_ALL = KeyBind.builder("reload_all", MENU_CATEGORY).build();
    public static final KeyBind MAPPING_STATS = KeyBind.builder("mapping_stats", MENU_CATEGORY).build();
    public static final KeyBind SEARCH_CLASS = KeyBind.builder("search_class", MENU_CATEGORY).mod(KeyEvent.SHIFT_DOWN_MASK).key(KeyEvent.VK_SPACE).build();
    public static final KeyBind SEARCH_METHOD = KeyBind.builder("search_method", MENU_CATEGORY).build();
    public static final KeyBind SEARCH_FIELD = KeyBind.builder("search_field", MENU_CATEGORY).build();

    private static final List<KeyBind> DEFAULT_KEY_BINDS = Stream.of(EXIT, DIALOG_SAVE, QUICK_FIND_DIALOG_NEXT,
            QUICK_FIND_DIALOG_PREVIOUS, SEARCH_DIALOG_NEXT, SEARCH_DIALOG_PREVIOUS, EDITOR_RENAME, EDITOR_PASTE,
            EDITOR_EDIT_JAVADOC, EDITOR_SHOW_INHERITANCE, EDITOR_SHOW_IMPLEMENTATIONS, EDITOR_SHOW_CALLS,
            EDITOR_SHOW_CALLS_SPECIFIC, EDITOR_OPEN_ENTRY, EDITOR_OPEN_PREVIOUS, EDITOR_OPEN_NEXT,
            EDITOR_TOGGLE_MAPPING, EDITOR_ZOOM_IN, EDITOR_ZOOM_OUT, EDITOR_CLOSE_TAB, EDITOR_RELOAD_CLASS,
            EDITOR_QUICK_FIND, SAVE_MAPPINGS, DROP_MAPPINGS, RELOAD_MAPPINGS, RELOAD_ALL, MAPPING_STATS, SEARCH_CLASS,
            SEARCH_METHOD, SEARCH_FIELD).map(KeyBind::toImmutable).toList();

    private static final List<KeyBind> CONFIGURABLE_KEY_BINDS = List.of(EDITOR_RENAME, EDITOR_PASTE, EDITOR_EDIT_JAVADOC,
            EDITOR_SHOW_INHERITANCE, EDITOR_SHOW_IMPLEMENTATIONS, EDITOR_SHOW_CALLS, EDITOR_SHOW_CALLS_SPECIFIC,
            EDITOR_OPEN_ENTRY, EDITOR_OPEN_PREVIOUS, EDITOR_OPEN_NEXT, EDITOR_TOGGLE_MAPPING, EDITOR_ZOOM_IN,
            EDITOR_ZOOM_OUT, EDITOR_CLOSE_TAB, EDITOR_RELOAD_CLASS, SAVE_MAPPINGS, DROP_MAPPINGS, RELOAD_MAPPINGS,
            RELOAD_ALL, MAPPING_STATS, SEARCH_CLASS, SEARCH_METHOD, SEARCH_FIELD);
    // Editing entries in CONFIGURABLE_KEY_BINDS directly wouldn't allow to revert the changes instead of saving
    private static List<KeyBind> EDITABLE_KEY_BINDS;

    private KeyBinds() {
    }

    public static boolean isConfigurable(KeyBind keyBind) {
        return CONFIGURABLE_KEY_BINDS.stream().anyMatch(bind -> bind.isSameKeyBind(keyBind));
    }

    public static Map<String, List<KeyBind>> getEditableKeyBindsByCategory() {
        return EDITABLE_KEY_BINDS.stream()
                .collect(Collectors.groupingBy(KeyBind::category));
    }

    public static void loadConfig() {
        for (KeyBind keyBind : CONFIGURABLE_KEY_BINDS) {
            keyBind.deserializeCombinations(KeyBindsConfig.getKeyBindCodes(keyBind));
        }

        resetEditableKeyBinds();
    }

    public static void saveConfig() {
        boolean modified = false;
        for (int i = 0; i < CONFIGURABLE_KEY_BINDS.size(); i++) {
            KeyBind keyBind = CONFIGURABLE_KEY_BINDS.get(i);
            KeyBind editedKeyBind = EDITABLE_KEY_BINDS.get(i);
            if (!editedKeyBind.equals(keyBind)) {
                modified = true;
                keyBind.setFrom(editedKeyBind);
                KeyBindsConfig.setKeyBind(editedKeyBind);
            }
        }
        if (modified) {
            KeyBindsConfig.save();
        }
    }

    // Reset the key binds to the saved values
    public static void resetEditableKeyBinds() {
        EDITABLE_KEY_BINDS = CONFIGURABLE_KEY_BINDS.stream().map(KeyBind::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static void resetToDefault(KeyBind keyBind) {
        // Ensure the key bind is editable
        if (!EDITABLE_KEY_BINDS.contains(keyBind)) {
            return;
        }

        KeyBind defaultKeyBind = DEFAULT_KEY_BINDS.stream().filter(bind -> bind.isSameKeyBind(keyBind)).findFirst().orElse(null);
        if (defaultKeyBind == null) {
            throw new IllegalStateException("Could not find default key bind for " + keyBind);
        }

        keyBind.setFrom(defaultKeyBind);
    }

    public static List<KeyBind> getEditableKeyBinds() {
        return EDITABLE_KEY_BINDS;
    }
}
