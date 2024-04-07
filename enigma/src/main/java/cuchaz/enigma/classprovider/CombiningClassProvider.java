package cuchaz.enigma.classprovider;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.objectweb.asm.tree.ClassNode;

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
	public Collection<String> getClassNames() {
		return Arrays.stream(classProviders)
			.map(ClassProvider::getClassNames)
			.flatMap(Collection::stream)
			.collect(Collectors.toSet());
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
