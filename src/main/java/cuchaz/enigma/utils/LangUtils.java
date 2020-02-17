package cuchaz.enigma.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.gson.Gson;

import cuchaz.enigma.config.Config;

public class LangUtils {
	public static final String DEFAULT_LANGUAGE = "en_us";
	private static final Gson GSON = new Gson();
	private static Map<String, String> translations = Maps.newHashMap();
	private static Map<String, String> defaultTranslations = Maps.newHashMap();
	private static Map<String, String> languageNames = Maps.newHashMap();
	
	static {
		putInCache(Config.getInstance().language);
	}
	
	@SuppressWarnings("unchecked")
	public static void putInCache(String language) {
		try (InputStream inputStream = LangUtils.class.getResourceAsStream("/lang/" + language + ".json")) {
			if (inputStream == null) {
				return;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
				if (language.equals(DEFAULT_LANGUAGE)) {
					defaultTranslations = GSON.fromJson(reader, Map.class);
				} else {
					translations = GSON.fromJson(reader, Map.class);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String translate(String key) {
		String value = translations.get(key);
		if (value != null) {
			return value;
		}
		value = defaultTranslations.get(key);
		if (value != null) {
			return value;
		}
		return key;
	}
	
	public static String getLanguageName(String language) {
		return languageNames.get(language);
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
				String fileName = file.getName().replace(".json", "");
				list.add(fileName);
				try (InputStream inputStream = LangUtils.class.getResourceAsStream("/lang/" + file.getName())) {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
						Map<?, ?> map = GSON.fromJson(reader, Map.class);
						languageNames.put(fileName, map.get("language").toString());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}
}
