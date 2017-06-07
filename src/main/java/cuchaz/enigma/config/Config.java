package cuchaz.enigma.config;

import com.google.common.io.Files;
import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class Config {

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

	public boolean useSystemLAF;
	public boolean useDraculaLAF;

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
		this.obfuscatedColor = 0xFFDCDC;
		this.obfuscatedHiglightAlpha = 1.0F;
		this.obfuscatedColorOutline = 0xA05050;
		this.obfuscatedOutlineAlpha = 1.0F;
		this.deobfuscatedColor = 0xDCFFDC;
		this.deobfuscatedHiglightAlpha = 1.0F;
		this.deobfuscatedColorOutline = 0x50A050;
		this.deobfuscatedOutlineAlpha = 1.0F;
		this.otherColorOutline = 0xB4B4B4;
		this.otherOutlineAlpha = 1.0F;
		this.editorBackground = 0xFFFFFF;
		this.highlightColor = 0x3333EE;
		this.stringColor = 0xCC6600;
		this.numberColor = 0x999933;
		this.operatorColor = 0x000000;
		this.delimiterColor = 0x000000;
		this.typeColor = 0x000000;
		this.identifierColor = 0x000000;
		this.defaultTextColor = 0x000000;
		this.useSystemLAF = true;
		this.useDraculaLAF = false;
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
