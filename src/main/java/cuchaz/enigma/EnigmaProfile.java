package cuchaz.enigma;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import cuchaz.enigma.api.service.EnigmaServiceType;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;

public final class EnigmaProfile {
	public static final EnigmaProfile EMPTY = new EnigmaProfile(ImmutableMap.of());

	private static final Gson GSON = new Gson();

	@SerializedName("services")
	private final Map<String, Service> serviceProfiles;

	private EnigmaProfile(Map<String, Service> serviceProfiles) {
		this.serviceProfiles = serviceProfiles;
	}

	public static EnigmaProfile parse(Reader reader) {
		return GSON.fromJson(reader, EnigmaProfile.class);
	}

	@Nullable
	public Service getServiceProfile(EnigmaServiceType<?> serviceType) {
		return serviceProfiles.get(serviceType.key);
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
}
