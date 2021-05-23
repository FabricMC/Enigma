package cuchaz.enigma.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.gson.Gson;

public class I18n {
	public static final String DEFAULT_LANGUAGE = "en_us";
	private static final Gson GSON = new Gson();
	private static Map<String, String> defaultTranslations = load(DEFAULT_LANGUAGE);
	private static Map<String, String> translations = defaultTranslations;
	private static Map<String, String> languageNames = Maps.newHashMap();

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

	public static String translateOrNull(String key) {
		String value = translations.get(key);
		if (value != null) return value;

		return defaultTranslations.get(key);
	}

	public static String translate(String key) {
		String tr = translateOrNull(key);
		return tr != null ? tr : key;
	}

	public static String translateOrEmpty(String key, Object... args) {
		String text = translateOrNull(key);
		if (text != null) {
			return String.format(text, args);
		} else {
			return "";
		}
	}

	public static String translateFormatted(String key, Object... args) {
		String text = translateOrNull(key);
		if (text != null) {
			return String.format(text, args);
		} else if (args.length == 0) {
			return key;
		} else {
			return key + Arrays.stream(args).map(Objects::toString).collect(Collectors.joining(", ", "[", "]"));
		}
	}

	public static String getLanguageName(String language) {
		return languageNames.get(language);
	}

	public static void setLanguage(String language) {
		translations = load(language);
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
