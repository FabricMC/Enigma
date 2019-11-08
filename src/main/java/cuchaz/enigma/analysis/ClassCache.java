package cuchaz.enigma.analysis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import cuchaz.enigma.CompiledSource;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.bytecode.translators.LocalVariableFixVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ClassCache implements AutoCloseable, CompiledSource {
	private final FileSystem fileSystem;
	private final ImmutableSet<String> classNames;

	private final Cache<String, ClassNode> nodeCache = CacheBuilder.newBuilder()
			.maximumSize(128)
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.build();

	private ClassCache(FileSystem fileSystem, ImmutableSet<String> classNames) {
		this.fileSystem = fileSystem;
		this.classNames = classNames;
	}

	public static ClassCache of(Path jarPath) throws IOException {
		FileSystem fileSystem = FileSystems.newFileSystem(jarPath, (ClassLoader) null);
		ImmutableSet<String> classNames = collectClassNames(fileSystem);

		return new ClassCache(fileSystem, classNames);
	}

	private static ImmutableSet<String> collectClassNames(FileSystem fileSystem) throws IOException {
		ImmutableSet.Builder<String> classNames = ImmutableSet.builder();
		for (Path root : fileSystem.getRootDirectories()) {
			Files.walk(root).map(Path::toString)
					.forEach(path -> {
						if (path.endsWith(".class")) {
							String name = path.substring(1, path.length() - ".class".length());
							classNames.add(name);
						}
					});
		}

		return classNames.build();
	}

	@Nullable
	@Override
	public ClassNode getClassNode(String name) {
		if (!classNames.contains(name)) {
			return null;
		}

		try {
			return nodeCache.get(name, () -> parseNode(name));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private ClassNode parseNode(String name) throws IOException {
		ClassReader reader = getReader(name);

		ClassNode node = new ClassNode();

		LocalVariableFixVisitor visitor = new LocalVariableFixVisitor(Opcodes.ASM5, node);
		reader.accept(visitor, 0);

		return node;
	}

	private ClassReader getReader(String name) throws IOException {
		Path path = fileSystem.getPath(name + ".class");
		byte[] bytes = Files.readAllBytes(path);
		return new ClassReader(bytes);
	}

	public int getClassCount() {
		return classNames.size();
	}

	public void visit(Supplier<ClassVisitor> visitorSupplier, int readFlags) {
		for (String className : classNames) {
			ClassVisitor visitor = visitorSupplier.get();

			ClassNode cached = nodeCache.getIfPresent(className);
			if (cached != null) {
				cached.accept(visitor);
				continue;
			}

			try {
				ClassReader reader = getReader(className);
				reader.accept(visitor, readFlags);
			} catch (IOException e) {
				System.out.println("Failed to visit class " + className);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() throws IOException {
		this.fileSystem.close();
	}

	public JarIndex index(ProgressListener progress) {
		JarIndex index = JarIndex.empty();
		index.indexJar(this, progress);
		return index;
	}
}
