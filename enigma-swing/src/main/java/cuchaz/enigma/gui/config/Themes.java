package cuchaz.enigma.gui.config;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import de.sciss.syntaxpane.DefaultSyntaxKit;

import cuchaz.enigma.gui.EnigmaSyntaxKit;
import cuchaz.enigma.gui.events.ThemeChangeListener;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.source.RenamableTokenType;

public class Themes {

	private static final Set<ThemeChangeListener> listeners = new HashSet<>();

	public static void setLookAndFeel(LookAndFeel lookAndFeel) {
		UiConfig.setLookAndFeel(lookAndFeel);
		updateTheme();
		UiConfig.save();
	}

	public static void updateTheme() {
		LookAndFeel laf = UiConfig.getLookAndFeel();
		laf.setGlobalLAF();
		UiConfig.setLookAndFeelDefaults(laf, LookAndFeel.isDarkLaf());
		EnigmaSyntaxKit.invalidate();
		DefaultSyntaxKit.initKit();
		DefaultSyntaxKit.registerContentType("text/enigma-sources", EnigmaSyntaxKit.class.getName());
		ImmutableMap<RenamableTokenType, BoxHighlightPainter> boxHighlightPainters = getBoxHighlightPainters();
		listeners.forEach(l -> l.onThemeChanged(laf, boxHighlightPainters));
		ScaleUtil.applyScaling();
		UiConfig.save();
	}

	public static ImmutableMap<RenamableTokenType, BoxHighlightPainter> getBoxHighlightPainters() {
		return ImmutableMap.of(
				RenamableTokenType.OBFUSCATED, BoxHighlightPainter.create(UiConfig.getObfuscatedColor(), UiConfig.getObfuscatedOutlineColor()),
				RenamableTokenType.PROPOSED, BoxHighlightPainter.create(UiConfig.getProposedColor(), UiConfig.getProposedOutlineColor()),
				RenamableTokenType.DEOBFUSCATED, BoxHighlightPainter.create(UiConfig.getDeobfuscatedColor(), UiConfig.getDeobfuscatedOutlineColor())
		);
	}

	public static void addListener(ThemeChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(ThemeChangeListener listener) {
		listeners.remove(listener);
	}

}
