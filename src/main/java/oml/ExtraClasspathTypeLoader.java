package oml;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * Copy of ClasspathTypeLoader supporting a classpath constructor.
 */
public class ExtraClasspathTypeLoader implements ITypeLoader {
	private final ClassLoader _loader;

	public ExtraClasspathTypeLoader(String extraClasspath){
		_loader = new URLClassLoader(Arrays.stream(extraClasspath.split(File.pathSeparator)).map(path-> {
			try {
				return new File(path).toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}).toArray(URL[]::new));
	}

	@Override
	public boolean tryLoadType(final String internalName, final Buffer buffer) {

		final String path = internalName.concat(".class");
		final URL resource = _loader.getResource(path);

		if (resource == null) {
			return false;
		}

		try (final InputStream stream = _loader.getResourceAsStream(path)) {
			final byte[] temp = new byte[4096];

			int bytesRead;

			while ((bytesRead = stream.read(temp, 0, temp.length)) > 0) {
				//buffer.ensureWriteableBytes(bytesRead);
				buffer.putByteArray(temp, 0, bytesRead);
			}

			buffer.flip();


			return true;
		}
		catch (final IOException ignored) {
			return false;
		}
	}
}
