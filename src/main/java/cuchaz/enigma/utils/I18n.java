package cuchaz.enigma.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.gson.Gson;

import cuchaz.enigma.config.Config;
import cuchaz.enigma.config.SimpleConfig;

public class I18n {
	public static final String DEFAULT_LANGUAGE = "en_us";
	private static final Gson GSON = new Gson();
	private static Config config = SimpleConfig.INSTANCE;
	private static Map<String, String> translations = Maps.newHashMap();
	private static Map<String, String> defaultTranslations = Maps.newHashMap();
	private static Map<String, String> languageNames = Maps.newHashMap();
	
	static {
		translations = load(config.getLanguage());
		defaultTranslations = load(DEFAULT_LANGUAGE);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> load(String language) {
		try (InputStream inputStream = I18n.class.getResourceAsStream("/lang/" + language + ".json")) {
			if (inputStream != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
					return GSON.fromJson(reader, Map.class);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyMap();
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

	/**
	 * Sets the current config instance and reloads the translations based on that config.
	 *
	 * @param config the new config instance
	 */
	public static void setConfig(Config config) {
		I18n.config = config;
		translations = load(config.getLanguage());
	}
	
	public static void setLanguage(String language) {
		config.setLanguage(language);
		try {
			config.saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> getAvailableLanguages() {
		ArrayList<String> list = new ArrayList<String>();
		
		try {
			ImmutableList<ResourceInfo> resources = ClassPath.from(Thread.currentThread().getContextClassLoader()).getResources().asList();
			Stream<ResourceInfo> dirStream = resources.stream();
			dirStream.forEach(context -> {
				String file = context.getResourceName();
				if (file.startsWith("lang/") && file.endsWith(".json")) {
					String fileName = file.substring(5, file.length() - 5);
					list.add(fileName);
					loadLanguageName(fileName);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	private static void loadLanguageName(String fileName) {
		try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("lang/" + fileName + ".json")) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				Map<?, ?> map = GSON.fromJson(reader, Map.class);
				languageNames.put(fileName, map.get("language").toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
