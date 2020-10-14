package cuchaz.enigma.classprovider;

import org.objectweb.asm.tree.ClassNode;

import org.jetbrains.annotations.Nullable;

/**
 * Combines a list of {@link ClassProvider}s into one, calling each one in a row
 * until one can provide the class.
 */
public class CombiningClassProvider implements ClassProvider {
    private final ClassProvider[] classProviders;

    public CombiningClassProvider(ClassProvider... classProviders) {
        this.classProviders = classProviders;
    }

    @Override
    @Nullable
    public ClassNode get(String name) {
        for (ClassProvider cp : classProviders) {
            ClassNode node = cp.get(name);

            if (node != null) {
                return node;
            }
        }

        return null;
    }
}
