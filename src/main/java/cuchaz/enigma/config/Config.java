package cuchaz.enigma.config;

import com.google.gson.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Created by Mark on 04/06/2017.
 */
public class Config {

    public static Config INSTANCE = new Config();

    public Integer obfuscatedColor = 0xFFDCDC;
    public Integer obfuscatedColorOutline = 0xA05050;

    public Integer deobfuscatedColor = 0xDCFFDC;
    public Integer deobfuscatedColorOutline = 0x50A050;

    public Integer editorBackground = 0xFFFFFF;

    public boolean useSystemLAF = true;

    public static void loadConfig() throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapter(Integer.class, new IntSerializer()).registerTypeAdapter(Integer.class, new IntDeserializer()).setPrettyPrinting().create();
        File configFile = new File(".engimaConfig.json");
        if (configFile.exists()) {
            INSTANCE = gson.fromJson(FileUtils.readFileToString(configFile, Charset.defaultCharset()), Config.class);
        }
        FileUtils.writeStringToFile(configFile, gson.toJson(INSTANCE), Charset.defaultCharset());
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

}
