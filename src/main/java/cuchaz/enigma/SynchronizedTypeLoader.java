package cuchaz.enigma;

import com.strobel.assembler.metadata.Buffer;
import cuchaz.enigma.mapping.entry.ClassEntry;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

/**
 * Typeloader with synchronized tryLoadType method
 */
public class SynchronizedTypeLoader implements ITranslatingTypeLoader {
	private final TranslatingTypeLoader delegate;

	public SynchronizedTypeLoader(TranslatingTypeLoader delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<String> getClassNamesToTry(String className) {
		return delegate.getClassNamesToTry(className);
	}

	@Override
	public List<String> getClassNamesToTry(ClassEntry obfClassEntry) {
		return delegate.getClassNamesToTry(obfClassEntry);
	}

	@Override
	public String transformInto(ClassNode node, ClassWriter writer) {
		return delegate.transformInto(node, writer);
	}

	@Override
	public synchronized boolean tryLoadType(String internalName, Buffer buffer) {
		return delegate.tryLoadType(internalName, buffer);
	}
}
