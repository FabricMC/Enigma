package cuchaz.enigma.config;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.MinecraftSyntaxKit;
import cuchaz.enigma.gui.highlight.DeobfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.ObfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.OtherHighlightPainter;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import java.awt.*;
import java.io.IOException;

public class Themes {

    public static void setDefault(Gui gui) {
        //TODO set to default
	    try {
		    Config.getInstance().reset();
	    } catch (IOException e) {
		    e.printStackTrace();
	    }
	    updateTheme(gui);
    }

    public static void setDark(Gui gui) {
        //Based off colors found here: https://github.com/dracula/dracula-theme/
        Config.getInstance().obfuscatedColor = 0xFF5555;
        Config.getInstance().obfuscatedHiglightAlpha = 0.3F;
        Config.getInstance().obfuscatedColorOutline = 0xFF5555;
        Config.getInstance().obfuscatedOutlineAlpha = 0.5F;
        Config.getInstance().deobfuscatedColor = 0x50FA7B;
        Config.getInstance().deobfuscatedHiglightAlpha = 0.3F;
        Config.getInstance().deobfuscatedColorOutline = 0x50FA7B;
        Config.getInstance().deobfuscatedOutlineAlpha = 0.5F;
        Config.getInstance().otherColorOutline = 0xB4B4B4;
        Config.getInstance().otherOutlineAlpha = 0.0F;
        Config.getInstance().editorBackground = 0x282A36;
        Config.getInstance().highlightColor = 0xFF79C6;
        Config.getInstance().stringColor = 0xF1FA8C;
        Config.getInstance().numberColor = 0xBD93F9;
        Config.getInstance().operatorColor = 0xF8F8F2;
        Config.getInstance().delimiterColor = 0xF8F8F2;
        Config.getInstance().typeColor = 0xF8F8F2;
        Config.getInstance().identifierColor = 0xF8F8F2;
        Config.getInstance().defaultTextColor = 0xF8F8F2;
        updateTheme(gui);
    }

    public static void updateTheme(Gui gui) {
        try {
	        Config.getInstance().saveConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MinecraftSyntaxKit.invalidate();
        DefaultSyntaxKit.initKit();
        DefaultSyntaxKit.registerContentType("text/minecraft", MinecraftSyntaxKit.class.getName());
        gui.obfuscatedHighlightPainter = new ObfuscatedHighlightPainter();
        gui.deobfuscatedHighlightPainter = new DeobfuscatedHighlightPainter();
        gui.otherHighlightPainter = new OtherHighlightPainter();
        gui.editor.updateUI();
        gui.editor.setBackground(new Color(Config.getInstance().editorBackground));
        gui.getController().refreshCurrentClass();
    }

}
