package cuchaz.enigma.gui.config;

import java.awt.Font;

import cuchaz.enigma.gui.config.legacy.Config;

public final class OldConfigImporter {

	private OldConfigImporter() {
	}

	@SuppressWarnings("deprecation")
	public static void doImport() {
		if (Config.CONFIG_FILE.exists()) {
			Config config = new Config();
			if (config.editorFont != null) {
				UiConfig.setEditorFont(Font.decode(config.editorFont));
			}
			UiConfig.setLanguage(config.language);
			UiConfig.setLookAndFeel(config.lookAndFeel);
			UiConfig.setScaleFactor(config.scaleFactor);
			UiConfig.setDecompiler(config.decompiler);
		}
	}

}
