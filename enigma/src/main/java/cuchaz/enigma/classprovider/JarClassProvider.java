package cuchaz.enigma.classprovider;

import com.google.common.collect.ImmutableSet;
import cuchaz.enigma.utils.AsmUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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
            ClassNode classNode = AsmUtil.bytesToNode(Files.readAllBytes(fileSystem.getPath(name + ".class")));
            fixRecordComponents(classNode);
            return classNode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fixRecordComponents(ClassNode classNode) {
        if (classNode.superName.equals("java/lang/Record") && classNode.recordComponents == null) {
            for (FieldNode field : classNode.fields) {
                if (Modifier.isStatic(field.access)) {
                    continue;
                }

                classNode.visitRecordComponent(field.name, field.desc, field.signature);
            }

            classNode.access |= Opcodes.ACC_RECORD;
        }
    }

    @Override
    public void close() throws Exception {
        fileSystem.close();
    }
}
