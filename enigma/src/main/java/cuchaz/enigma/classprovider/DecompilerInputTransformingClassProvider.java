package cuchaz.enigma.classprovider;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.EnigmaServices;
import cuchaz.enigma.api.service.DecompilerInputTransformerService;

public class DecompilerInputTransformingClassProvider implements ClassProvider {
	private final ClassProvider delegate;
	private final EnigmaServices services;

	public DecompilerInputTransformingClassProvider(ClassProvider delegate, EnigmaServices services) {
		this.delegate = delegate;
		this.services = services;
	}

	@Override
	public Collection<String> getClassNames() {
		return delegate.getClassNames();
	}

	@Override
	@Nullable
	public ClassNode get(String name) {
		ClassNode classNode = delegate.get(name);

		if (classNode == null) {
			return null;
		}

		for (DecompilerInputTransformerService transformer : services.get(DecompilerInputTransformerService.TYPE)) {
			classNode = transformer.transform(classNode);
		}

		return classNode;
	}
}
