package cuchaz.enigma.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import cuchaz.enigma.EnigmaServices;
import cuchaz.enigma.api.service.I18nService;

public class I18n {
	public static final String DEFAULT_LANGUAGE = "en_us";
	private static final Gson GSON = new Gson();
	private static Map<String, String> defaultTranslations = Map.of();
	private static Map<String, String> translations = Map.of();
	private static final Map<String, String> LANGUAGE_NAMES = new HashMap<>();

	public static void initialize(EnigmaServices services) {
		translations = defaultTranslations = load(DEFAULT_LANGUAGE, services);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> load(String language, EnigmaServices services) {
		Map<String, String> translations = new HashMap<>();

		for (I18nService i18nService : services.get(I18nService.TYPE)) {
			try (InputStream inputStream = i18nService.getTranslationResource(language)) {
				if (inputStream != null) {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
						translations.putAll(GSON.fromJson(reader, Map.class));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return translations;
	}

	public static String translateOrNull(String key) {
		String value = translations.get(key);

		if (value != null) {
			return value;
		}

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
		return LANGUAGE_NAMES.get(language);
	}

	public static void setLanguage(String language, EnigmaServices services) {
		translations = load(language, services);
	}

	public static ArrayList<String> getAvailableLanguages() {
		ArrayList<String> list = new ArrayList<>();

		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			List<String> availableTranslations;

			try (InputStream is = cl.getResourceAsStream("lang/index.txt")) {
				if (is == null) {
					// This scenario should only really happen when launching from an IDE that does not run the necessary gradle tasks
					throw new IOException("Resource 'lang/index.txt' not found");
				}

				availableTranslations = Arrays.asList(
						new String(is.readAllBytes(), StandardCharsets.UTF_8)
								.split("\n")
				);
			}

			availableTranslations.forEach(fileName -> {
				list.add(fileName);
				loadLanguageName(fileName);
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
				LANGUAGE_NAMES.put(fileName, map.get("language").toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
