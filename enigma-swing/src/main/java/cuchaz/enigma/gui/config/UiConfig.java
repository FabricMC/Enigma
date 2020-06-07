package cuchaz.enigma.gui.config;

import java.awt.Color;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.utils.I18n;

public final class UiConfig {

	private UiConfig() {
	}

	// General UI configuration such as localization
	private static final ConfigContainer ui = ConfigContainer.getOrCreate("enigma/enigmaui");
	// Swing specific configuration such as theming
	private static final ConfigContainer swing = ConfigContainer.getOrCreate("enigma/enigmaswing");

	public static void save() {
		ui.save();
		swing.save();
	}

	public static String getLanguage() {
		return ui.data().section("General").setIfAbsentString("Language", I18n.DEFAULT_LANGUAGE);
	}

	public static void setLanguage(String language) {
		ui.data().section("General").setString("Language", language);
	}

	public static float getScaleFactor() {
		return (float) swing.data().section("General").setIfAbsentDouble("Scale Factor", 1.0);
	}

	public static void setScaleFactor(float scale) {
		swing.data().section("General").setDouble("Scale Factor", scale);
	}

	public static LookAndFeel getLookAndFeel() {
		return swing.data().section("Themes").setIfAbsentEnum(LookAndFeel::valueOf, "Current", LookAndFeel.NONE);
	}

	public static void setLookAndFeel(LookAndFeel laf) {
		swing.data().section("Themes").setEnum("Current", laf);
	}

	public static Decompiler getDecompiler() {
		return ui.data().section("Decompiler").setIfAbsentEnum(Decompiler::valueOf, "Current", Decompiler.PROCYON);
	}

	public static void setDecompiler(Decompiler d) {
		ui.data().section("Decompiler").setEnum("Current", d);
	}

	private static Color fromComponents(int rgb, double alpha) {
		int rgba = rgb & 0xFFFFFF | (int) (alpha * 255) << 24;
		return new Color(rgba, true);
	}

	private static Color getThemeColorRgba(String colorName) {
		ConfigSection s = swing.data().section("Themes").section(getLookAndFeel().getName());
		return fromComponents(s.getRgbColor(colorName).orElse(0), s.getDouble(String.format("%s Alpha", colorName)).orElse(0));
	}

	private static Color getThemeColorRgb(String colorName) {
		ConfigSection s = swing.data().section("Themes").section(getLookAndFeel().getName());
		return new Color(s.getRgbColor(colorName).orElse(0));
	}

	public static Color getObfuscatedColor() {
		return getThemeColorRgba("Obfuscated Color");
	}

	public static Color getObfuscatedOutlineColor() {
		return getThemeColorRgba("Obfuscated Outline Color");
	}

	public static Color getProposedColor() {
		return getThemeColorRgba("Proposed Color");
	}

	public static Color getProposedOutlineColor() {
		return getThemeColorRgba("Proposed Outline Color");
	}

	public static Color getDeobfuscatedColor() {
		return getThemeColorRgba("Deobfuscated Color");
	}

	public static Color getDeobfuscatedOutlineColor() {
		return getThemeColorRgba("Deobfuscated Outline Color");
	}

	public static Color getEditorBackgroundColor() {
		return getThemeColorRgb("Editor Background");
	}

	public static Color getHighlightColor() {
		return getThemeColorRgb("Highlight Color");
	}

	public static Color getCaretColor() {
		return getThemeColorRgb("Caret Color");
	}

	public static Color getSelectionHighlightColor() {
		return getThemeColorRgb("Selection Highlight Color");
	}

	public static Color getStringColor() {
		return getThemeColorRgb("String Color");
	}

	public static Color getNumberColor() {
		return getThemeColorRgb("Number Color");
	}

	public static Color getOperatorColor() {
		return getThemeColorRgb("Operator Color");
	}

	public static Color getDelimiterColor() {
		return getThemeColorRgb("Delimiter Color");
	}

	public static Color getTypeColor() {
		return getThemeColorRgb("Type Color");
	}

	public static Color getIdentifierColor() {
		return getThemeColorRgb("Identifier Color");
	}

	public static Color getTextColor() {
		return getThemeColorRgb("Text Color");
	}

	public static Color getLineNumbersForegroundColor() {
		return getThemeColorRgb("Line Numbers Foreground Color");
	}

	public static Color getLineNumbersBackgroundColor() {
		return getThemeColorRgb("Line Numbers Background Color");
	}

	public static Color getLineNumbersSelectedColor() {
		return getThemeColorRgb("Line Numbers Selected Color");
	}

	public static void setLookAndFeelDefaults(LookAndFeel laf, boolean isDark) {
		ConfigSection s = swing.data().section("Themes").section(laf.getName());
		if (!isDark) {
			// Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
			s.setIfAbsentRgbColor("Line Numbers Foreground Color", 0x333300);
			s.setIfAbsentRgbColor("Line Numbers Background Color", 0xEEEEFF);
			s.setIfAbsentRgbColor("Line Numbers Selected Color", 0xCCCCEE);
			s.setIfAbsentRgbColor("Obfuscated Color", 0xFFDCDC);
			s.setIfAbsentDouble("Obfuscated Color Alpha", 1.0);
			s.setIfAbsentRgbColor("Obfuscated Outline Color", 0xFFDCDC);
			s.setIfAbsentDouble("Obfuscated Outline Color Alpha", 1.0);
			s.setIfAbsentRgbColor("Proposed Color", 0x000000);
			s.setIfAbsentDouble("Proposed Color Alpha", 0.75);
			s.setIfAbsentRgbColor("Proposed Outline Color", 0x000000);
			s.setIfAbsentDouble("Proposed Outline Color Alpha", 0.15);
			s.setIfAbsentRgbColor("Deobfuscated Color", 0xDCFFDC);
			s.setIfAbsentDouble("Deobfuscated Color Alpha", 1.0);
			s.setIfAbsentRgbColor("Deobfuscated Outline Color", 0x50A050);
			s.setIfAbsentDouble("Deobfuscated Outline Color Alpha", 1.0);
			s.setIfAbsentRgbColor("Editor Background", 0xFFFFFF);
			s.setIfAbsentRgbColor("Highlight Color", 0x3333EE);
			s.setIfAbsentRgbColor("Caret Color", 0x000000);
			s.setIfAbsentRgbColor("Selection Highlight Color", 0x000000);
			s.setIfAbsentRgbColor("String Color", 0xCC6600);
			s.setIfAbsentRgbColor("Number Color", 0x999933);
			s.setIfAbsentRgbColor("Operator Color", 0x000000);
			s.setIfAbsentRgbColor("Delimiter Color", 0x000000);
			s.setIfAbsentRgbColor("Type Color", 0x000000);
			s.setIfAbsentRgbColor("Identifier Color", 0x000000);
			s.setIfAbsentRgbColor("Text Color", 0x000000);
		} else {
			// Based off colors found here: https://github.com/dracula/dracula-theme/
			s.setIfAbsentRgbColor("Line Numbers Foreground Color", 0xA4A4A3);
			s.setIfAbsentRgbColor("Line Numbers Background Color", 0x313335);
			s.setIfAbsentRgbColor("Line Numbers Selected Color", 0x606366);
			s.setIfAbsentRgbColor("Obfuscated Color", 0xFF5555);
			s.setIfAbsentDouble("Obfuscated Color Alpha", 0.3);
			s.setIfAbsentRgbColor("Obfuscated Outline Color", 0xFF5555);
			s.setIfAbsentDouble("Obfuscated Outline Color Alpha", 0.5);
			s.setIfAbsentRgbColor("Proposed Color", 0x606366);
			s.setIfAbsentDouble("Proposed Color Alpha", 0.3);
			s.setIfAbsentRgbColor("Proposed Outline Color", 0x606366);
			s.setIfAbsentDouble("Proposed Outline Color Alpha", 0.5);
			s.setIfAbsentRgbColor("Deobfuscated Color", 0x50FA7B);
			s.setIfAbsentDouble("Deobfuscated Color Alpha", 0.3);
			s.setIfAbsentRgbColor("Deobfuscated Outline Color", 0x50FA7B);
			s.setIfAbsentDouble("Deobfuscated Outline Color Alpha", 0.5);
			s.setIfAbsentRgbColor("Editor Background", 0x282A36);
			s.setIfAbsentRgbColor("Highlight Color", 0xFF79C6);
			s.setIfAbsentRgbColor("Caret Color", 0xF8F8F2);
			s.setIfAbsentRgbColor("Selection Highlight Color", 0xF8F8F2);
			s.setIfAbsentRgbColor("String Color", 0xF1FA8C);
			s.setIfAbsentRgbColor("Number Color", 0xBD93F9);
			s.setIfAbsentRgbColor("Operator Color", 0xF8F8F2);
			s.setIfAbsentRgbColor("Delimiter Color", 0xF8F8F2);
			s.setIfAbsentRgbColor("Type Color", 0xF8F8F2);
			s.setIfAbsentRgbColor("Identifier Color", 0xF8F8F2);
			s.setIfAbsentRgbColor("Text Color", 0xF8F8F2);
		}
	}

}
