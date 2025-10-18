package cuchaz.enigma.classprovider;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.analysis.IndexClassWriter;
import cuchaz.enigma.analysis.index.EntryIndex;

public class AddFramesIfNecessaryClassProvider implements ClassProvider {
	private final ClassProvider delegate;
	private final EntryIndex entryIndex;

	public AddFramesIfNecessaryClassProvider(ClassProvider delegate, EntryIndex entryIndex) {
		this.delegate = delegate;
		this.entryIndex = entryIndex;
	}

	@Override
	public Collection<String> getClassNames() {
		return delegate.getClassNames();
	}

	@Override
	@Nullable
	public ClassNode get(String name) {
		ClassNode clazz = delegate.get(name);

		if (clazz == null) {
			return null;
		}

		if ((clazz.version & 0xffff) >= Opcodes.V1_7) {
			// already has frames
			return clazz;
		}

		IndexClassWriter cw = new IndexClassWriter(entryIndex, ClassWriter.COMPUTE_FRAMES);
		clazz.accept(cw);
		ClassReader cr = new ClassReader(cw.toByteArray());
		ClassNode node = new ClassNode();
		cr.accept(node, 0);
		return node;
	}
}
