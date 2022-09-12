package cuchaz.enigma.gui.events;

import java.util.Map;

import cuchaz.enigma.gui.config.LookAndFeel;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.source.RenamableTokenType;

public interface ThemeChangeListener {
	void onThemeChanged(LookAndFeel lookAndFeel, Map<RenamableTokenType, BoxHighlightPainter> boxHighlightPainters);
}
