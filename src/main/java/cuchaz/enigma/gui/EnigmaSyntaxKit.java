package cuchaz.enigma.gui;

import cuchaz.enigma.gui.config.GuiConfig;
import de.sciss.syntaxpane.components.LineNumbersRuler;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import de.sciss.syntaxpane.util.Configuration;

public class EnigmaSyntaxKit extends JavaSyntaxKit {
    private static Configuration configuration = null;

    @Override
    public Configuration getConfig() {
        if(configuration == null){
            initConfig(super.getConfig(JavaSyntaxKit.class));
        }
        return configuration;
    }

    public void initConfig(Configuration baseConfig){
        configuration = baseConfig;
        //See de.sciss.syntaxpane.TokenType
        configuration.put("Style.KEYWORD", GuiConfig.getInstance().highlightColor + ", 0");
        configuration.put("Style.KEYWORD2", GuiConfig.getInstance().highlightColor + ", 3");
        configuration.put("Style.STRING", GuiConfig.getInstance().stringColor + ", 0");
        configuration.put("Style.STRING2", GuiConfig.getInstance().stringColor + ", 1");
        configuration.put("Style.NUMBER", GuiConfig.getInstance().numberColor + ", 1");
        configuration.put("Style.OPERATOR", GuiConfig.getInstance().operatorColor + ", 0");
        configuration.put("Style.DELIMITER", GuiConfig.getInstance().delimiterColor + ", 1");
        configuration.put("Style.TYPE", GuiConfig.getInstance().typeColor + ", 2");
        configuration.put("Style.TYPE2", GuiConfig.getInstance().typeColor + ", 1");
        configuration.put("Style.IDENTIFIER", GuiConfig.getInstance().identifierColor + ", 0");
        configuration.put("Style.DEFAULT", GuiConfig.getInstance().defaultTextColor + ", 0");
        configuration.put(LineNumbersRuler.PROPERTY_BACKGROUND, GuiConfig.getInstance().lineNumbersBackground + "");
        configuration.put(LineNumbersRuler.PROPERTY_FOREGROUND, GuiConfig.getInstance().lineNumbersForeground + "");
        configuration.put(LineNumbersRuler.PROPERTY_CURRENT_BACK, GuiConfig.getInstance().lineNumbersSelected + "");
        configuration.put("RightMarginColumn", "999"); //No need to have a right margin, if someone wants it add a config

        configuration.put("Action.quick-find", "cuchaz.enigma.gui.QuickFindAction, menu F");
    }

    public static void invalidate(){
        configuration = null;
    }
}
