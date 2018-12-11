package cuchaz.enigma.config;

import com.bulenkov.darcula.DarculaLaf;
import com.google.common.io.Files;
import com.google.gson.*;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class Config {
	public static class AlphaColorEntry {
		public Integer rgb;
		public float alpha = 1.0f;

		public AlphaColorEntry(Integer rgb, float alpha) {
			this.rgb = rgb;
			this.alpha = alpha;
		}

		public Color get() {
			if (rgb == null) {
				return new Color(0, 0, 0, 0);
			}

			Color baseColor = new Color(rgb);
			return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(255 * alpha));
		}
	}

    public enum LookAndFeel {
		DEFAULT("Default"),
		DARCULA("Dank");

		private final String name;

		LookAndFeel(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setGlobalLAF() {
			try {
				switch (this) {
					case DEFAULT:
						UIManager.setLookAndFeel(new MetalLookAndFeel());
						break;
					case DARCULA:
						UIManager.setLookAndFeel(new DarculaLaf());
						break;
				}
			} catch (Exception e){
				throw new Error("Failed to set global look and feel", e);
			}
		}

		public void apply(Config config) {
			switch (this) {
				case DEFAULT:
					//Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
					config.lineNumbersForeground = 0x333300;
					config.lineNumbersBackground = 0xEEEEFF;
					config.lineNumbersSelected = 0xCCCCEE;
					config.obfuscatedColor = new AlphaColorEntry(0xFFDCDC, 1.0f);
					config.obfuscatedColorOutline = new AlphaColorEntry(0xA05050, 1.0f);
					config.proposedColor = new AlphaColorEntry(0x000000, 0.075f);
					config.proposedColorOutline = new AlphaColorEntry(0x000000, 0.15f);
					config.deobfuscatedColor = new AlphaColorEntry(0xDCFFDC, 1.0f);
					config.deobfuscatedColorOutline = new AlphaColorEntry(0x50A050, 1.0f);
					config.otherColorOutline = new AlphaColorEntry(0xB4B4B4, 1.0f);
					config.editorBackground = 0xFFFFFF;
					config.highlightColor = 0x3333EE;
					config.caretColor = 0x000000;
					config.selectionHighlightColor = 0x000000;
					config.stringColor = 0xCC6600;
					config.numberColor = 0x999933;
					config.operatorColor = 0x000000;
					config.delimiterColor = 0x000000;
					config.typeColor = 0x000000;
					config.identifierColor = 0x000000;
					config.defaultTextColor = 0x000000;
					break;
				case DARCULA:
					//Based off colors found here: https://github.com/dracula/dracula-theme/
					config.lineNumbersForeground = 0xA4A4A3;
					config.lineNumbersBackground = 0x313335;
					config.lineNumbersSelected = 0x606366;
					config.obfuscatedColor = new AlphaColorEntry(0xFF5555, 0.3f);
					config.obfuscatedColorOutline = new AlphaColorEntry(0xFF5555, 0.5f);
					config.deobfuscatedColor = new AlphaColorEntry(0x50FA7B, 0.3f);
					config.deobfuscatedColorOutline = new AlphaColorEntry(0x50FA7B, 0.5f);
					config.proposedColor = new AlphaColorEntry(0x606366, 0.3f);
					config.proposedColorOutline = new AlphaColorEntry(0x606366, 0.5f);
					config.otherColorOutline = new AlphaColorEntry(0xB4B4B4, 0.0f);
					config.editorBackground = 0x282A36;
					config.highlightColor = 0xFF79C6;
					config.caretColor = 0xF8F8F2;
					config.selectionHighlightColor = 0xF8F8F2;
					config.stringColor = 0xF1FA8C;
					config.numberColor = 0xBD93F9;
					config.operatorColor = 0xF8F8F2;
					config.delimiterColor = 0xF8F8F2;
					config.typeColor = 0xF8F8F2;
					config.identifierColor = 0xF8F8F2;
					config.defaultTextColor = 0xF8F8F2;
					break;
			}
		}
	}

	private static final File DIR_HOME = new File(System.getProperty("user.home"));
	private static final File ENIGMA_DIR = new File(DIR_HOME, ".enigma");
	private static final File CONFIG_FILE = new File(ENIGMA_DIR, "config.json");
	private static final Config INSTANCE = new Config();

	private final transient Gson gson; // transient to exclude it from being exposed

	public AlphaColorEntry obfuscatedColor;
	public AlphaColorEntry obfuscatedColorOutline;
	public AlphaColorEntry proposedColor;
	public AlphaColorEntry proposedColorOutline;
	public AlphaColorEntry deobfuscatedColor;
	public AlphaColorEntry deobfuscatedColorOutline;
	public AlphaColorEntry otherColorOutline;

	public Integer editorBackground;
	public Integer highlightColor;
	public Integer caretColor;
	public Integer selectionHighlightColor;

	public Integer stringColor;
	public Integer numberColor;
	public Integer operatorColor;
	public Integer delimiterColor;
	public Integer typeColor;
	public Integer identifierColor;
	public Integer defaultTextColor;

	public Integer lineNumbersBackground;
	public Integer lineNumbersSelected;
	public Integer lineNumbersForeground;

	public LookAndFeel lookAndFeel = LookAndFeel.DEFAULT;

	private Config() {
		gson = new GsonBuilder()
			.registerTypeAdapter(Integer.class, new IntSerializer())
			.registerTypeAdapter(Integer.class, new IntDeserializer())
			.registerTypeAdapter(Config.class, (InstanceCreator<Config>) type -> this)
			.setPrettyPrinting()
			.create();
		try {
			this.loadConfig();
		} catch (IOException ignored) {
			try {
				this.reset();
			} catch (IOException ignored1) {
			}
		}
	}

	public void loadConfig() throws IOException {
		if (!ENIGMA_DIR.exists()) ENIGMA_DIR.mkdirs();
		File configFile = new File(ENIGMA_DIR, "config.json");
		boolean loaded = false;

		if (configFile.exists()) {
			try {
				gson.fromJson(Files.asCharSource(configFile, Charset.defaultCharset()).read(), Config.class);
				loaded = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!loaded) {
			this.reset();
			Files.touch(configFile);
		}
		saveConfig();
	}

	public void saveConfig() throws IOException {
		Files.asCharSink(CONFIG_FILE, Charset.defaultCharset()).write(gson.toJson(this));
	}

	public void reset() throws IOException {
		this.lookAndFeel = LookAndFeel.DEFAULT;
		this.lookAndFeel.apply(this);
		this.saveConfig();
	}

	private static class IntSerializer implements JsonSerializer<Integer> {
		public JsonElement serialize(Integer src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive("#" + Integer.toHexString(src).toUpperCase());
		}
	}

	private static class IntDeserializer implements JsonDeserializer<Integer> {
		public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			return (int) Long.parseLong(json.getAsString().replace("#", ""), 16);
		}
	}

	public static Config getInstance() {
		return INSTANCE;
	}
}
