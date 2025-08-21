package cuchaz.enigma.classprovider;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.utils.AsmUtil;

/**
 * Provides classes by loading them from a JAR file.
 */
public class JarClassProvider implements AutoCloseable, ClassProvider {
	private final FileSystem fileSystem;
	private final Set<String> classNames;

	public JarClassProvider(Path jarPath) throws IOException {
		this.fileSystem = FileSystems.newFileSystem(jarPath, (ClassLoader) null);
		this.classNames = collectClassNames(fileSystem);
	}

	private static Set<String> collectClassNames(FileSystem fileSystem) throws IOException {
		Set<String> classNames = new HashSet<>();

		for (Path root : fileSystem.getRootDirectories()) {
			try (Stream<Path> stream = Files.walk(root)) {
				stream.map(Path::toString).forEach(path -> {
					if (path.endsWith(".class")) {
						String name = path.substring(1, path.length() - ".class".length());
						classNames.add(name);
					}
				});
			}
		}

		return Collections.unmodifiableSet(classNames);
	}

	@Override
	public Set<String> getClassNames() {
		return classNames;
	}

	@Nullable
	@Override
	public ClassNode get(String name) {
		if (!classNames.contains(name)) {
			return null;
		}

		try {
			return AsmUtil.bytesToNode(Files.readAllBytes(fileSystem.getPath(name + ".class")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		fileSystem.close();
	}
}
