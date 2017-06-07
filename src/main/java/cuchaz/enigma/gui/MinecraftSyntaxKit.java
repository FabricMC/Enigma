package cuchaz.enigma.gui;

import cuchaz.enigma.config.Config;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import de.sciss.syntaxpane.util.Configuration;

public class MinecraftSyntaxKit extends JavaSyntaxKit {
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
        configuration.put("RightMarginColumn", "999"); //No need to have a right margin, if someone wants it add a config
    }

    public static void invalidate(){
        configuration = null;
    }
}
