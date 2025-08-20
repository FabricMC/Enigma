package cuchaz.enigma;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;

public final class EnigmaProfile {
	public static final EnigmaProfile EMPTY = new EnigmaProfile();

	private static final MappingSaveParameters DEFAULT_MAPPING_SAVE_PARAMETERS = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
	private static final Gson GSON = new Gson();

	@SerializedName("disabled_plugins")
	private final Set<String> disabledPlugins = Set.of();

	@SerializedName("mapping_save_parameters")
	private final MappingSaveParameters mappingSaveParameters = DEFAULT_MAPPING_SAVE_PARAMETERS;

	private EnigmaProfile() {
	}

	public static EnigmaProfile read(@Nullable Path file) throws IOException {
		if (file != null) {
			try (BufferedReader reader = Files.newBufferedReader(file)) {
				return EnigmaProfile.parse(reader);
			}
		} else {
			return EMPTY;
		}
	}

	public static EnigmaProfile parse(Reader reader) {
		return GSON.fromJson(reader, EnigmaProfile.class);
	}

	public Set<String> getDisabledPlugins() {
		return disabledPlugins;
	}

	public MappingSaveParameters getMappingSaveParameters() {
		return mappingSaveParameters;
	}
}
