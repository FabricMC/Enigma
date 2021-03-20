package cuchaz.enigma.gui;

import de.sciss.syntaxpane.DefaultSyntaxKit;
import de.sciss.syntaxpane.components.LineNumbersRuler;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import de.sciss.syntaxpane.util.Configuration;

import cuchaz.enigma.gui.config.UiConfig;

import java.awt.Font;

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
        // Javascript and causes the editor to freeze on first load for a short
        // time.
        configuration.keySet().removeIf(s -> s.startsWith("Action.") &&
                !(s.startsWith("Action.find") ||
                        s.startsWith("Action.goto-line") ||
                        s.startsWith("Action.jump-to-pair") ||
                        s.startsWith("Action.quick-find")));

		// See de.sciss.syntaxpane.TokenType
		configuration.put("Style.KEYWORD", String.format("%d, 0", UiConfig.getHighlightColor().getRGB()));
		configuration.put("Style.KEYWORD2", String.format("%d, 3", UiConfig.getHighlightColor().getRGB()));
		configuration.put("Style.STRING", String.format("%d, 0", UiConfig.getStringColor().getRGB()));
		configuration.put("Style.STRING2", String.format("%d, 1", UiConfig.getStringColor().getRGB()));
		configuration.put("Style.NUMBER", String.format("%d, 1", UiConfig.getNumberColor().getRGB()));
		configuration.put("Style.OPERATOR", String.format("%d, 0", UiConfig.getOperatorColor().getRGB()));
		configuration.put("Style.DELIMITER", String.format("%d, 1", UiConfig.getDelimiterColor().getRGB()));
		configuration.put("Style.TYPE", String.format("%d, 2", UiConfig.getTypeColor().getRGB()));
		configuration.put("Style.TYPE2", String.format("%d, 1", UiConfig.getTypeColor().getRGB()));
		configuration.put("Style.IDENTIFIER", String.format("%d, 0", UiConfig.getIdentifierColor().getRGB()));
		configuration.put("Style.DEFAULT", String.format("%d, 0", UiConfig.getTextColor().getRGB()));
		configuration.put(LineNumbersRuler.PROPERTY_BACKGROUND, String.format("%d", UiConfig.getLineNumbersBackgroundColor().getRGB()));
		configuration.put(LineNumbersRuler.PROPERTY_FOREGROUND, String.format("%d", UiConfig.getLineNumbersForegroundColor().getRGB()));
		configuration.put(LineNumbersRuler.PROPERTY_CURRENT_BACK, String.format("%d", UiConfig.getLineNumbersSelectedColor().getRGB()));
		configuration.put("RightMarginColumn", "999"); //No need to have a right margin, if someone wants it add a config

		configuration.put("Action.quick-find", "cuchaz.enigma.gui.QuickFindAction, menu F");

		Font editorFont = UiConfig.shouldUseCustomFonts() ? UiConfig.getEditorFont() : UiConfig.getFallbackEditorFont();
		configuration.put("DefaultFont", UiConfig.encodeFont(editorFont));
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
