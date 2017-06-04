package cuchaz.enigma.gui;

import cuchaz.enigma.config.Config;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import de.sciss.syntaxpane.util.Configuration;

/**
 * Created by Mark on 04/06/2017.
 */
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
        configuration.put("Style.KEYWORD", Config.INSTANCE.highlightColor + ", 0");
        configuration.put("Style.KEYWORD2", Config.INSTANCE.highlightColor + ", 3");
        configuration.put("Style.STRING", Config.INSTANCE.stringColor + ", 0");
        configuration.put("Style.STRING2", Config.INSTANCE.stringColor + ", 1");
        configuration.put("Style.NUMBER", Config.INSTANCE.numberColor + ", 1");
        configuration.put("Style.OPERATOR", Config.INSTANCE.operatorColor + ", 0");
        configuration.put("Style.DELIMITER", Config.INSTANCE.delimiterColor + ", 1");
        configuration.put("Style.TYPE", Config.INSTANCE.typeColor + ", 2");
        configuration.put("Style.TYPE2", Config.INSTANCE.typeColor + ", 1");
        configuration.put("Style.IDENTIFIER", Config.INSTANCE.identifierColor + ", 0");
        configuration.put("Style.DEFAULT", Config.INSTANCE.defaultTextColor + ", 0");
        configuration.put("RightMarginColumn", "999"); //No need to have a right margin, if someone wants it add a config
    }

    public static void invalidate(){
        configuration = null;
    }
}
