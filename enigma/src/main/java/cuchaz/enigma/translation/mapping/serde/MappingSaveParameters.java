package cuchaz.enigma.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

public class MappingSaveParameters {
	@SerializedName("file_name_format")
	private final MappingFileNameFormat fileNameFormat;

	public MappingSaveParameters(MappingFileNameFormat fileNameFormat) {
		this.fileNameFormat = fileNameFormat;
	}

	public MappingFileNameFormat getFileNameFormat() {
		return fileNameFormat;
	}
}
