package cuchaz.enigma.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ServiceLoader;

import cuchaz.enigma.api.EnigmaIcon;

public interface IconLoadingService {
	IconLoadingService INSTANCE = ServiceLoader.load(IconLoadingService.class).findFirst()
			.orElseThrow(() -> new IllegalStateException("Trying to load icon on headless Enigma"));

	EnigmaIcon loadIcon(InputStream in) throws IOException;
}
