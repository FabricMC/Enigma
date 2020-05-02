package cuchaz.enigma.gui.events;

import java.util.Map;

import cuchaz.enigma.gui.config.Config.LookAndFeel;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.gui.highlight.TokenHighlightType;

public interface ThemeChangeListener {

	void onThemeChanged(LookAndFeel lookAndFeel, Map<TokenHighlightType, BoxHighlightPainter> boxHighlightPainters);

}
