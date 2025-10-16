package cuchaz.enigma.api;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.utils.IconLoadingService;

@ApiStatus.NonExtendable
public interface EnigmaIcon {
	/**
	 * Loads an icon resource from the given SVG resource path.
	 *
	 * @param resource the path to the resource to be loaded
	 * @return The loaded icon
	 * @throws IOException if the resource could not be found or an error occurred while reading it
	 */
	static EnigmaIcon loadResource(String resource) throws IOException {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("Could not find resource: " + resource);
			}

			return load(in);
		}
	}

	/**
	 * Loads an icon in SVG format from the given input stream.
	 *
	 * @param in the input stream to load from
	 * @return The loaded icon
	 * @throws IOException if the stream throws an error
	 */
	static EnigmaIcon load(InputStream in) throws IOException {
		return IconLoadingService.INSTANCE.loadIcon(in);
	}
}
