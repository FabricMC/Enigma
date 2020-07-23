package cuchaz.enigma.gui.config;

import java.awt.*;
import java.util.Optional;
import java.util.OptionalInt;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.gui.util.ScaleUtil;
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
		ConfigSection s = swing.data().section("Themes").section(getLookAndFeel().getName()).section("Colors");
		return fromComponents(s.getRgbColor(colorName).orElse(0), s.getDouble(String.format("%s Alpha", colorName)).orElse(0));
	}

	private static Color getThemeColorRgb(String colorName) {
		ConfigSection s = swing.data().section("Themes").section(getLookAndFeel().getName()).section("Colors");
		return new Color(s.getRgbColor(colorName).orElse(0));
	}

	public static Color getObfuscatedColor() {
		return getThemeColorRgba("Obfuscated");
	}

	public static Color getObfuscatedOutlineColor() {
		return getThemeColorRgba("Obfuscated Outline");
	}

	public static Color getProposedColor() {
		return getThemeColorRgba("Proposed");
	}

	public static Color getProposedOutlineColor() {
		return getThemeColorRgba("Proposed Outline");
	}

	public static Color getDeobfuscatedColor() {
		return getThemeColorRgba("Deobfuscated");
	}

	public static Color getDeobfuscatedOutlineColor() {
		return getThemeColorRgba("Deobfuscated Outline");
	}

	public static Color getEditorBackgroundColor() {
		return getThemeColorRgb("Editor Background");
	}

	public static Color getHighlightColor() {
		return getThemeColorRgb("Highlight");
	}

	public static Color getCaretColor() {
		return getThemeColorRgb("Caret");
	}

	public static Color getSelectionHighlightColor() {
		return getThemeColorRgb("Selection Highlight");
	}

	public static Color getStringColor() {
		return getThemeColorRgb("String");
	}

	public static Color getNumberColor() {
		return getThemeColorRgb("Number");
	}

	public static Color getOperatorColor() {
		return getThemeColorRgb("Operator");
	}

	public static Color getDelimiterColor() {
		return getThemeColorRgb("Delimiter");
	}

	public static Color getTypeColor() {
		return getThemeColorRgb("Type");
	}

	public static Color getIdentifierColor() {
		return getThemeColorRgb("Identifier");
	}

	public static Color getTextColor() {
		return getThemeColorRgb("Text");
	}

	public static Color getLineNumbersForegroundColor() {
		return getThemeColorRgb("Line Numbers Foreground");
	}

	public static Color getLineNumbersBackgroundColor() {
		return getThemeColorRgb("Line Numbers Background");
	}

	public static Color getLineNumbersSelectedColor() {
		return getThemeColorRgb("Line Numbers Selected");
	}

	public static Font getFont(String name, Font fallback) {
		Optional<String> spec = swing.data().section("Fonts").getString(name);
		return spec.map(Font::decode).orElse(fallback);
	}

	public static void setFont(String name, Font font) {
		swing.data().section("Fonts").setString(name, encodeFont(font));
	}

	public static Font getEditorFont() {
		return getFont("Editor", ScaleUtil.scaleFont(Font.decode(Font.MONOSPACED)));
	}

	public static void setEditorFont(Font font) {
		setFont("Editor", font);
	}

	public static String encodeFont(Font font) {
		int style = font.getStyle();
		String s = style == (Font.BOLD | Font.ITALIC) ? "bolditalic" : style == Font.ITALIC ? "italic" : style == Font.BOLD ? "bold" : "plain";
		return String.format("%s-%s-%s", font.getName(), s, font.getSize());
	}

	public static Dimension getWindowSize(String window, Dimension fallback) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		OptionalInt width = section.getInt(String.format("Width %s", screenSize.width));
		OptionalInt height = section.getInt(String.format("Height %s", screenSize.height));
		if (width.isPresent() && height.isPresent()) {
			return new Dimension(width.getAsInt(), height.getAsInt());
		} else {
			return fallback;
		}
	}

	public static void setWindowSize(String window, Dimension dim) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		section.setInt(String.format("Width %s", screenSize.width), dim.width);
		section.setInt(String.format("Height %s", screenSize.height), dim.height);
	}

	public static Point getWindowPos(String window, Point fallback) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		OptionalInt x = section.getInt(String.format("X %s", screenSize.width));
		OptionalInt y = section.getInt(String.format("Y %s", screenSize.height));
		if (x.isPresent() && y.isPresent()) {
			return new Point(x.getAsInt(), y.getAsInt());
		} else {
			return fallback;
		}
	}

	public static void setWindowPos(String window, Point rect) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		section.setInt(String.format("X %s", screenSize.width), rect.x);
		section.setInt(String.format("Y %s", screenSize.height), rect.y);
	}

	public static String getLastSelectedDir() {
		return swing.data().section("File Dialog").getString("Selected").orElse("");
	}

	public static void setLastSelectedDir(String directory) {
		swing.data().section("File Dialog").setString("Selected", directory);
	}

	public static void setLookAndFeelDefaults(LookAndFeel laf, boolean isDark) {
		ConfigSection s = swing.data().section("Themes").section(laf.getName()).section("Colors");
		if (!isDark) {
			// Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
			s.setIfAbsentRgbColor("Line Numbers Foreground", 0x333300);
			s.setIfAbsentRgbColor("Line Numbers Background", 0xEEEEFF);
			s.setIfAbsentRgbColor("Line Numbers Selected", 0xCCCCEE);
			s.setIfAbsentRgbColor("Obfuscated", 0xFFDCDC);
			s.setIfAbsentDouble("Obfuscated Alpha", 1.0);
			s.setIfAbsentRgbColor("Obfuscated Outline", 0xA05050);
			s.setIfAbsentDouble("Obfuscated Outline Alpha", 1.0);
			s.setIfAbsentRgbColor("Proposed", 0x000000);
			s.setIfAbsentDouble("Proposed Alpha", 0.15);
			s.setIfAbsentRgbColor("Proposed Outline", 0x000000);
			s.setIfAbsentDouble("Proposed Outline Alpha", 0.75);
			s.setIfAbsentRgbColor("Deobfuscated", 0xDCFFDC);
			s.setIfAbsentDouble("Deobfuscated Alpha", 1.0);
			s.setIfAbsentRgbColor("Deobfuscated Outline", 0x50A050);
			s.setIfAbsentDouble("Deobfuscated Outline Alpha", 1.0);
			s.setIfAbsentRgbColor("Editor Background", 0xFFFFFF);
			s.setIfAbsentRgbColor("Highlight", 0x3333EE);
			s.setIfAbsentRgbColor("Caret", 0x000000);
			s.setIfAbsentRgbColor("Selection Highlight", 0x000000);
			s.setIfAbsentRgbColor("String", 0xCC6600);
			s.setIfAbsentRgbColor("Number", 0x999933);
			s.setIfAbsentRgbColor("Operator", 0x000000);
			s.setIfAbsentRgbColor("Delimiter", 0x000000);
			s.setIfAbsentRgbColor("Type", 0x000000);
			s.setIfAbsentRgbColor("Identifier", 0x000000);
			s.setIfAbsentRgbColor("Text", 0x000000);
		} else {
			// Based off colors found here: https://github.com/dracula/dracula-theme/
			s.setIfAbsentRgbColor("Line Numbers Foreground", 0xA4A4A3);
			s.setIfAbsentRgbColor("Line Numbers Background", 0x313335);
			s.setIfAbsentRgbColor("Line Numbers Selected", 0x606366);
			s.setIfAbsentRgbColor("Obfuscated", 0xFF5555);
			s.setIfAbsentDouble("Obfuscated Alpha", 0.3);
			s.setIfAbsentRgbColor("Obfuscated Outline", 0xFF5555);
			s.setIfAbsentDouble("Obfuscated Outline Alpha", 0.5);
			s.setIfAbsentRgbColor("Proposed", 0x606366);
			s.setIfAbsentDouble("Proposed Alpha", 0.3);
			s.setIfAbsentRgbColor("Proposed Outline", 0x606366);
			s.setIfAbsentDouble("Proposed Outline Alpha", 0.5);
			s.setIfAbsentRgbColor("Deobfuscated", 0x50FA7B);
			s.setIfAbsentDouble("Deobfuscated Alpha", 0.3);
			s.setIfAbsentRgbColor("Deobfuscated Outline", 0x50FA7B);
			s.setIfAbsentDouble("Deobfuscated Outline Alpha", 0.5);
			s.setIfAbsentRgbColor("Editor Background", 0x282A36);
			s.setIfAbsentRgbColor("Highlight", 0xFF79C6);
			s.setIfAbsentRgbColor("Caret", 0xF8F8F2);
			s.setIfAbsentRgbColor("Selection Highlight", 0xF8F8F2);
			s.setIfAbsentRgbColor("String", 0xF1FA8C);
			s.setIfAbsentRgbColor("Number", 0xBD93F9);
			s.setIfAbsentRgbColor("Operator", 0xF8F8F2);
			s.setIfAbsentRgbColor("Delimiter", 0xF8F8F2);
			s.setIfAbsentRgbColor("Type", 0xF8F8F2);
			s.setIfAbsentRgbColor("Identifier", 0xF8F8F2);
			s.setIfAbsentRgbColor("Text", 0xF8F8F2);
		}
	}
}
