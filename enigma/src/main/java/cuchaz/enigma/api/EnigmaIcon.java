package cuchaz.enigma.api;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.utils.IconLoadingService;

@ApiStatus.NonExtendable
public interface EnigmaIcon {
	static EnigmaIcon loadResource(String resource) throws IOException {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("Could not find resource: " + resource);
			}

			return load(in);
		}
	}

	static EnigmaIcon load(InputStream in) throws IOException {
		return IconLoadingService.INSTANCE.loadIcon(in);
	}
}
