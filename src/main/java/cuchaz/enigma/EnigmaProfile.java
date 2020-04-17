package cuchaz.enigma;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import cuchaz.enigma.api.service.EnigmaServiceType;
import cuchaz.enigma.translation.mapping.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnigmaProfile {
	public static final EnigmaProfile EMPTY = new EnigmaProfile(new ServiceContainer(ImmutableMap.of()));

	private static final MappingSaveParameters DEFAULT_MAPPING_SAVE_PARAMETERS = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ServiceContainer.class, (JsonDeserializer<ServiceContainer>) EnigmaProfile::loadServiceContainer)
			.create();
	private static final Type SERVICE_LIST_TYPE = new TypeToken<List<Service>>() {
	}.getType();

	@SerializedName("services")
	private final ServiceContainer serviceProfiles;

	@SerializedName("mapping_save_parameters")
	private final MappingSaveParameters mappingSaveParameters = null;

	private EnigmaProfile(ServiceContainer serviceProfiles) {
		this.serviceProfiles = serviceProfiles;
	}

	public static EnigmaProfile read(@Nullable Path file) throws IOException {
		if (file != null) {
			try (BufferedReader reader = Files.newBufferedReader(file)) {
				return EnigmaProfile.parse(reader);
			}
		} else {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/profile.json"), StandardCharsets.UTF_8))){
				return EnigmaProfile.parse(reader);
			} catch (IOException ex) {
				System.out.println("Failed to load default profile, will use empty profile: " + ex.getMessage());
				return EnigmaProfile.EMPTY;
			}
		}
	}

	public static EnigmaProfile parse(Reader reader) {
		return GSON.fromJson(reader, EnigmaProfile.class);
	}

	private static ServiceContainer loadServiceContainer(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (!json.isJsonObject()) {
			throw new JsonParseException("services must be an Object!");
		}

		JsonObject object = json.getAsJsonObject();

		ImmutableMap.Builder<String, List<Service>> builder = ImmutableMap.builder();

		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			JsonElement value = entry.getValue();
			if (value.isJsonObject()) {
				builder.put(entry.getKey(), Collections.singletonList(GSON.fromJson(value, Service.class)));
			} else if (value.isJsonArray()) {
				builder.put(entry.getKey(), GSON.fromJson(value, SERVICE_LIST_TYPE));
			} else {
				throw new JsonParseException(String.format("Don't know how to convert %s to a list of service!", value));
			}
		}

		return new ServiceContainer(builder.build());
	}

	public List<Service> getServiceProfiles(EnigmaServiceType<?> serviceType) {
		return serviceProfiles.get(serviceType.key);
	}

	public MappingSaveParameters getMappingSaveParameters() {
		//noinspection ConstantConditions
		return mappingSaveParameters == null ? EnigmaProfile.DEFAULT_MAPPING_SAVE_PARAMETERS : mappingSaveParameters;
	}

	public static class Service {
		private final String id;
		private final Map<String, String> args;

		Service(String id, Map<String, String> args) {
			this.id = id;
			this.args = args;
		}

		public boolean matches(String id) {
			return this.id.equals(id);
		}

		public Optional<String> getArgument(String key) {
			return args != null ? Optional.ofNullable(args.get(key)) : Optional.empty();
		}
	}

	static final class ServiceContainer {
		private final Map<String, List<Service>> services;

		ServiceContainer(Map<String, List<Service>> services) {
			this.services = services;
		}

		List<Service> get(String key) {
			return services.getOrDefault(key, Collections.emptyList());
		}
	}
}
