package cuchaz.enigma.gui.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import cuchaz.enigma.gui.EnigmaSyntaxKit;
import cuchaz.enigma.gui.events.ThemeChangeListener;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import cuchaz.enigma.gui.util.ScaleUtil;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class Themes {

	private static final Set<ThemeChangeListener> listeners = new HashSet<>();

	public static void setLookAndFeel(Config.LookAndFeel lookAndFeel) {
		Config.getInstance().lookAndFeel = lookAndFeel;
		updateTheme();
	}

	public static void updateTheme() {
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
		ImmutableMap<TokenHighlightType, BoxHighlightPainter> boxHighlightPainters = getBoxHighlightPainters();
		listeners.forEach(l -> l.onThemeChanged(config.lookAndFeel, boxHighlightPainters));
		ScaleUtil.applyScaling();
	}

	public static ImmutableMap<TokenHighlightType, BoxHighlightPainter> getBoxHighlightPainters() {
		Config config = Config.getInstance();
		return ImmutableMap.of(
				TokenHighlightType.OBFUSCATED, BoxHighlightPainter.create(config.obfuscatedColor, config.obfuscatedColorOutline),
				TokenHighlightType.PROPOSED, BoxHighlightPainter.create(config.proposedColor, config.proposedColorOutline),
				TokenHighlightType.DEOBFUSCATED, BoxHighlightPainter.create(config.deobfuscatedColor, config.deobfuscatedColorOutline)
		);
	}

	public static void addListener(ThemeChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(ThemeChangeListener listener) {
		listeners.remove(listener);
	}

}
