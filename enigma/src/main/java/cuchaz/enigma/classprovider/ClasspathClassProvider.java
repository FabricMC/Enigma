package cuchaz.enigma.classprovider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides classes by loading them from the classpath.
 */
public class ClasspathClassProvider implements ClassProvider {
    @Nullable @Override public ClassNode get(String name) {
        try (InputStream in = ClasspathClassProvider.class.getResourceAsStream("/" + name + ".class")) {
            if (in == null) {
                return null;
            }

            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            return node;
        } catch (IOException e) {
            return null;
        }
    }
}
