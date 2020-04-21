package cuchaz.enigma.config;

import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.swing.SwingUtilities;

import com.github.swingdpi.UiDefaultsScaler;
import com.google.common.collect.ImmutableMap;
import cuchaz.enigma.gui.EnigmaSyntaxKit;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import cuchaz.enigma.gui.util.ScaleUtil;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class Themes {

	public static void setLookAndFeel(Gui gui, Config.LookAndFeel lookAndFeel) {
		Config.getInstance().lookAndFeel = lookAndFeel;
		updateTheme(gui);
	}

	public static void updateTheme(Gui gui) {
		Config config = Config.getInstance();
		config.lookAndFeel.setGlobalLAF();
		config.lookAndFeel.apply(config);
		try {
			config.saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		EnigmaSyntaxKit.invalidate();
		DefaultSyntaxKit.initKit();
		DefaultSyntaxKit.registerContentType("text/enigma-sources", EnigmaSyntaxKit.class.getName());
		gui.boxHighlightPainters = ImmutableMap.of(
				TokenHighlightType.OBFUSCATED, BoxHighlightPainter.create(config.obfuscatedColor, config.obfuscatedColorOutline),
				TokenHighlightType.PROPOSED, BoxHighlightPainter.create(config.proposedColor, config.proposedColorOutline),
				TokenHighlightType.DEOBFUSCATED, BoxHighlightPainter.create(config.deobfuscatedColor, config.deobfuscatedColorOutline)
		);
		gui.setEditorTheme(config.lookAndFeel);
		SwingUtilities.updateComponentTreeUI(gui.getFrame());
		ScaleUtil.applyScaling();
	}


}
