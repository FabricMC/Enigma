package cuchaz.enigma.config;

import com.github.swingdpi.UiDefaultsScaler;
import com.google.common.collect.ImmutableMap;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.EnigmaSyntaxKit;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import javax.swing.*;

import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Field;

public class Themes {

    public static void setLookAndFeel(Gui gui, Config.LookAndFeel lookAndFeel) {
        Config.getInstance().lookAndFeel = lookAndFeel;
	    updateTheme(gui);
    }

    public static void updateTheme(Gui gui) {
        Config config = Config.getInstance();
        config.lookAndFeel.setGlobalLAF();
        config.lookAndFeel.apply(config);
        UiDefaultsScaler.updateAndApplyGlobalScaling(150, false);
        try {
            config.saveConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EnigmaSyntaxKit.invalidate();
        DefaultSyntaxKit.initKit();
        try {
            Field $DEFAULT_FONT = DefaultSyntaxKit.class.getDeclaredField("DEFAULT_FONT");
            $DEFAULT_FONT.setAccessible(true);
            Font font = (Font) $DEFAULT_FONT.get(null);
            font = font.deriveFont(font.getSize() * 1.5f);
            $DEFAULT_FONT.set(null, font);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        DefaultSyntaxKit.registerContentType("text/enigma-sources", EnigmaSyntaxKit.class.getName());
        gui.boxHighlightPainters = ImmutableMap.of(
                TokenHighlightType.OBFUSCATED, BoxHighlightPainter.create(config.obfuscatedColor, config.obfuscatedColorOutline),
                TokenHighlightType.PROPOSED, BoxHighlightPainter.create(config.proposedColor, config.proposedColorOutline),
                TokenHighlightType.DEOBFUSCATED, BoxHighlightPainter.create(config.deobfuscatedColor, config.deobfuscatedColorOutline)
        );
        gui.setEditorTheme(config.lookAndFeel);
        SwingUtilities.updateComponentTreeUI(gui.getFrame());
    }
}
