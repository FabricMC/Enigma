package cuchaz.enigma.config;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.MinecraftSyntaxKit;
import cuchaz.enigma.gui.highlight.DeobfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.ObfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.OtherHighlightPainter;
import cuchaz.enigma.mapping.Entry;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import java.awt.*;
import java.io.IOException;

/**
 * Created by Mark on 04/06/2017.
 */
public class Themes {

    public static void setDefault(Gui gui) {
        //TODO set to default
        Config.INSTANCE = new Config();
        updateTheme(gui);
    }

    public static void setDark(Gui gui) {
        //Based off colors found here: https://github.com/dracula/dracula-theme/
        Config.INSTANCE.obfuscatedColor = 0xFF5555;
        Config.INSTANCE.obfuscatedHiglightAlpha = 0.3F;
        Config.INSTANCE.obfuscatedColorOutline = 0xFF5555;
        Config.INSTANCE.obfuscatedOutlineAlpha = 0.5F;
        Config.INSTANCE.deobfuscatedColor = 0x50FA7B;
        Config.INSTANCE.deobfuscatedHiglightAlpha = 0.3F;
        Config.INSTANCE.deobfuscatedColorOutline = 0x50FA7B;
        Config.INSTANCE.deobfuscatedOutlineAlpha = 0.5F;
        Config.INSTANCE.otherColorOutline = 0xB4B4B4;
        Config.INSTANCE.otherOutlineAlpha = 0.0F;
        Config.INSTANCE.editorBackground = 0x282A36;
        Config.INSTANCE.highlightColor = 0xFF79C6;
        Config.INSTANCE.stringColor = 0xF1FA8C;
        Config.INSTANCE.numberColor = 0xBD93F9;
        Config.INSTANCE.operatorColor = 0xF8F8F2;
        Config.INSTANCE.delimiterColor = 0xF8F8F2;
        Config.INSTANCE.typeColor = 0xF8F8F2;
        Config.INSTANCE.identifierColor = 0xF8F8F2;
        Config.INSTANCE.defaultTextColor = 0xF8F8F2;
        updateTheme(gui);
    }

    public static void updateTheme(Gui gui) {
        try {
            Config.saveConfig();
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
        gui.editor.setBackground(new Color(Config.INSTANCE.editorBackground));
        gui.getController().refreshCurrentClass();
    }

}
