package cuchaz.enigma.config;

import com.bulenkov.darcula.DarculaLaf;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.MinecraftSyntaxKit;
import cuchaz.enigma.gui.highlight.DeobfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.ObfuscatedHighlightPainter;
import cuchaz.enigma.gui.highlight.OtherHighlightPainter;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Themes {

    public static void setLookAndFeel(Gui gui, Config.LookAndFeel lookAndFeel) {
        Config.getInstance().lookAndFeel = lookAndFeel;
	    updateTheme(gui);
    }

    public static void updateTheme(Gui gui) {
        Config.getInstance().lookAndFeel.apply(Config.getInstance());
        Config.getInstance().lookAndFeel.setGlobalLAF();
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
        gui.setEditorTheme(Config.getInstance().lookAndFeel);
        SwingUtilities.updateComponentTreeUI(gui.getFrame());
    }
}
