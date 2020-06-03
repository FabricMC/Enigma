package cuchaz.enigma.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

public enum MappingFileNameFormat {
	@SerializedName("by_obf")
	BY_OBF,
	@SerializedName("by_deobf")
	BY_DEOBF
}
