package cuchaz.enigma.config;

import com.bulenkov.darcula.DarculaLaf;
import com.google.common.io.Files;
import com.google.gson.*;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class Config {
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
					config.obfuscatedColor = 0xFFDCDC;
					config.obfuscatedHiglightAlpha = 1.0F;
					config.obfuscatedColorOutline = 0xA05050;
					config.obfuscatedOutlineAlpha = 1.0F;
					config.deobfuscatedColor = 0xDCFFDC;
					config.deobfuscatedHiglightAlpha = 1.0F;
					config.deobfuscatedColorOutline = 0x50A050;
					config.deobfuscatedOutlineAlpha = 1.0F;
					config.otherColorOutline = 0xB4B4B4;
					config.otherOutlineAlpha = 1.0F;
					config.editorBackground = 0xFFFFFF;
					config.highlightColor = 0x3333EE;
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
					config.obfuscatedColor = 0xFF5555;
					config.obfuscatedHiglightAlpha = 0.3F;
					config.obfuscatedColorOutline = 0xFF5555;
					config.obfuscatedOutlineAlpha = 0.5F;
					config.deobfuscatedColor = 0x50FA7B;
					config.deobfuscatedHiglightAlpha = 0.3F;
					config.deobfuscatedColorOutline = 0x50FA7B;
					config.deobfuscatedOutlineAlpha = 0.5F;
					config.otherColorOutline = 0xB4B4B4;
					config.otherOutlineAlpha = 0.0F;
					config.editorBackground = 0x282A36;
					config.highlightColor = 0xFF79C6;
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

	public Integer obfuscatedColor;
	public float obfuscatedHiglightAlpha;
	public Integer obfuscatedColorOutline;
	public float obfuscatedOutlineAlpha;
	public Integer deobfuscatedColor;
	public float deobfuscatedHiglightAlpha;
	public Integer deobfuscatedColorOutline;
	public float deobfuscatedOutlineAlpha;
	public Integer otherColorOutline;
	public float otherOutlineAlpha;

	//Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
	public Integer editorBackground;
	public Integer highlightColor;

	public Integer stringColor;
	public Integer numberColor;
	public Integer operatorColor;
	public Integer delimiterColor;
	public Integer typeColor;
	public Integer identifierColor;
	public Integer defaultTextColor;

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
		if (configFile.exists()) gson.fromJson(Files.asCharSource(configFile, Charset.defaultCharset()).read(), Config.class);
		else {
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
