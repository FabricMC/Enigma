package cuchaz.enigma.gui;

import de.sciss.syntaxpane.DefaultSyntaxKit;
import de.sciss.syntaxpane.components.LineNumbersRuler;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import de.sciss.syntaxpane.util.Configuration;

import cuchaz.enigma.gui.config.Config;

public class EnigmaSyntaxKit extends JavaSyntaxKit {

    private static Configuration configuration = null;

    @Override
    public Configuration getConfig() {
        if (configuration == null) {
            initConfig(DefaultSyntaxKit.getConfig(JavaSyntaxKit.class));
        }
        return configuration;
    }

    public void initConfig(Configuration baseConfig) {
        configuration = flattenConfiguration(baseConfig, EnigmaSyntaxKit.class);

        // Remove all actions except a select few because they disregard the
        // editable state of the editor, or at least are useless anyway because
        // they would try editing the file.
        // Also includes the Action.insert-date action which is written in
        // Javascript and causes the editor to freeze on first load.
        configuration.keySet().removeIf(s -> s.startsWith("Action.") &&
                !(s.startsWith("Action.find") ||
                        s.startsWith("Action.goto-line") ||
                        s.startsWith("Action.jump-to-pair") ||
                        s.startsWith("Action.quick-find")));

        // See de.sciss.syntaxpane.TokenType
        configuration.put("Style.KEYWORD", Config.getInstance().highlightColor + ", 0");
        configuration.put("Style.KEYWORD2", Config.getInstance().highlightColor + ", 3");
        configuration.put("Style.STRING", Config.getInstance().stringColor + ", 0");
        configuration.put("Style.STRING2", Config.getInstance().stringColor + ", 1");
        configuration.put("Style.NUMBER", Config.getInstance().numberColor + ", 1");
        configuration.put("Style.OPERATOR", Config.getInstance().operatorColor + ", 0");
        configuration.put("Style.DELIMITER", Config.getInstance().delimiterColor + ", 1");
        configuration.put("Style.TYPE", Config.getInstance().typeColor + ", 2");
        configuration.put("Style.TYPE2", Config.getInstance().typeColor + ", 1");
        configuration.put("Style.IDENTIFIER", Config.getInstance().identifierColor + ", 0");
        configuration.put("Style.DEFAULT", Config.getInstance().defaultTextColor + ", 0");
        configuration.put(LineNumbersRuler.PROPERTY_BACKGROUND, Config.getInstance().lineNumbersBackground + "");
        configuration.put(LineNumbersRuler.PROPERTY_FOREGROUND, Config.getInstance().lineNumbersForeground + "");
        configuration.put(LineNumbersRuler.PROPERTY_CURRENT_BACK, Config.getInstance().lineNumbersSelected + "");
        configuration.put("RightMarginColumn", "999"); //No need to have a right margin, if someone wants it add a config

        configuration.put("Action.quick-find", "cuchaz.enigma.gui.QuickFindAction, menu F");
    }

    /**
     * Creates a new configuration from the passed configuration so that it has
     * no parents and all its values are on the same level. This is needed since
     * there is no way to remove map entries from parent configurations.
     *
     * @param source      the configuration to flatten
     * @param configClass the class for the new configuration
     * @return a new configuration
     */
    private static Configuration flattenConfiguration(Configuration source, Class<?> configClass) {
        Configuration config = new Configuration(configClass, null);
        for (String p : source.stringPropertyNames()) {
            config.put(p, source.getString(p));
        }
        return config;
    }

    public static void invalidate() {
        configuration = null;
    }

}
