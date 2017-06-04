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
    public float obfuscatedHiglightAlpha = 1.0F;
    public Integer obfuscatedColorOutline = 0xA05050;
    public float obfuscatedOutlineAlpha = 1.0F;

    public Integer deobfuscatedColor = 0xDCFFDC;
    public float deobfuscatedHiglightAlpha = 1.0F;
    public Integer deobfuscatedColorOutline = 0x50A050;
    public float deobfuscatedOutlineAlpha = 1.0F;

    public Integer otherColorOutline = 0xB4B4B4;
    public float otherOutlineAlpha = 1.0F;

    //Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
    public Integer editorBackground = 0xFFFFFF;
    public Integer highlightColor = 0x3333EE;
    public Integer stringColor = 0xCC6600;
    public Integer numberColor = 0x999933;
    public Integer operatorColor = 0x000000;
    public Integer delimiterColor = 0x000000;
    public Integer typeColor = 0x000000;
    public Integer identifierColor = 0x000000;
    public Integer defaultTextColor = 0x000000;

    public boolean useSystemLAF = true;

    public static void loadConfig() throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapter(Integer.class, new IntSerializer()).registerTypeAdapter(Integer.class, new IntDeserializer()).setPrettyPrinting().create();
        File dirHome = new File(System.getProperty("user.home"));
        File engimaDir = new File(dirHome, ".enigma");
        File configFile = new File(engimaDir, "config.json");
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
