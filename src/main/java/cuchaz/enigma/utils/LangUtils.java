package cuchaz.enigma.utils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.io.Resources;
import com.google.gson.Gson;

import cuchaz.enigma.config.Config;

public class LangUtils {
	public static final String DEFAULT_LANGUAGE = "en_us";
	private static final Gson GSON = new Gson();
	
	public static String translate(String key) {
		return translate(key, Config.getInstance().language);
	}
	
	public static String translate(String key, String language) {
		Path path = Paths.get("src/main/resources/lang/" + language + ".json");
		Path defaultPath = Paths.get("src/main/resources/lang/" + DEFAULT_LANGUAGE + ".json");
		Reader reader;
		String value;
		try {
			reader = Files.newBufferedReader(path);
			value = getValue(key, reader);
			if (key.equals(value)) {
				reader = Files.newBufferedReader(defaultPath);
				value = getValue(key, reader);
			}
			reader.close();
			return value;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return key;
	}
	
	private static String getValue(String key, Reader reader) {
		Map<?, ?> map = GSON.fromJson(reader, Map.class);
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (key.equals(entry.getKey())) {
				return entry.getValue().toString();
			}
		}
		return key;
	}
	
	public static void setLanguage(String language) {
		Config.getInstance().language = language;
		try {
			Config.getInstance().saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> getAvailableLanguages() {
		ArrayList<String> list = new ArrayList<String>();
		File dir = new File(Resources.getResource("lang").getFile());
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".json")) {
				list.add(file.getName().replace(".json", ""));
			}
		}
		return list;
	}
}
