package cuchaz.enigma.gui.config.legacy;

import java.awt.Color;
import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import cuchaz.enigma.gui.config.Decompiler;
import cuchaz.enigma.utils.I18n;

@Deprecated
public class Config {
	public static class AlphaColorEntry {
		public Integer rgb;
		public float alpha;

		public AlphaColorEntry(Integer rgb, float alpha) {
			this.rgb = rgb;
			this.alpha = alpha;
		}

		public Color get() {
			if (rgb == null) {
				return new Color(0, 0, 0, 0);
			}

			Color baseColor = new Color(rgb);
			return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (255 * alpha));
		}
	}

	private static final Path DIR_HOME = Path.of(System.getProperty("user.home"));
	private static final Path ENIGMA_DIR = DIR_HOME.resolve(".enigma");
	public static final Path CONFIG_FILE = ENIGMA_DIR.resolve("config.json");

	private final transient Gson gson; // transient to exclude it from being exposed

	public AlphaColorEntry obfuscatedColor;
	public AlphaColorEntry obfuscatedColorOutline;
	public AlphaColorEntry proposedColor;
	public AlphaColorEntry proposedColorOutline;
	public AlphaColorEntry deobfuscatedColor;
	public AlphaColorEntry deobfuscatedColorOutline;

	public String editorFont;

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

	public String language = I18n.DEFAULT_LANGUAGE;

	public cuchaz.enigma.gui.config.LookAndFeel lookAndFeel = cuchaz.enigma.gui.config.LookAndFeel.DEFAULT;

	public float scaleFactor = 1.0f;

	public Decompiler decompiler = Decompiler.VINEFLOWER;

	public Config() {
		gson = new GsonBuilder().registerTypeAdapter(Integer.class, new IntSerializer()).registerTypeAdapter(Integer.class, new IntDeserializer()).registerTypeAdapter(Config.class, (InstanceCreator<Config>) type -> this).setPrettyPrinting().create();
		this.loadConfig();
	}

	public void loadConfig() {
		if (Files.exists(CONFIG_FILE)) {
			try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE)) {
				gson.fromJson(reader, Config.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class IntSerializer implements JsonSerializer<Integer> {
		@Override
		public JsonElement serialize(Integer src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive("#" + Integer.toHexString(src).toUpperCase(Locale.ROOT));
		}
	}

	private static class IntDeserializer implements JsonDeserializer<Integer> {
		@Override
		public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			return (int) Long.parseLong(json.getAsString().replace("#", ""), 16);
		}
	}
}
