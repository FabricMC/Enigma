package cuchaz.enigma.config;

import cuchaz.enigma.utils.I18n;

/**
 * A singleton {@link Config} instance for scenarios without a GUI.
 */
public final class SimpleConfig implements Config {
    public static final SimpleConfig INSTANCE = new SimpleConfig();

    private String language = I18n.DEFAULT_LANGUAGE;

    private SimpleConfig() {}

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public void saveConfig() {
        // Nothing to save when using Enigma as a library
    }
}
