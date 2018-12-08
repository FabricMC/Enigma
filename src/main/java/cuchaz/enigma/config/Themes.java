package cuchaz.enigma.config;

import com.google.common.collect.ImmutableMap;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.EnigmaSyntaxKit;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;
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
        EnigmaSyntaxKit.invalidate();
        DefaultSyntaxKit.initKit();
        DefaultSyntaxKit.registerContentType("text/enigma-sources", EnigmaSyntaxKit.class.getName());
        gui.boxHighlightPainters = ImmutableMap.of(
                "obfuscated", BoxHighlightPainter.create(Config.getInstance().obfuscatedColor, Config.getInstance().obfuscatedColorOutline),
                "proposed", BoxHighlightPainter.create(Config.getInstance().proposedColor, Config.getInstance().proposedColorOutline),
                "deobfuscated", BoxHighlightPainter.create(Config.getInstance().deobfuscatedColor, Config.getInstance().deobfuscatedColorOutline),
                "other", BoxHighlightPainter.create(null, Config.getInstance().otherColorOutline)
        );
        gui.setEditorTheme(Config.getInstance().lookAndFeel);
        SwingUtilities.updateComponentTreeUI(gui.getFrame());
    }
}
