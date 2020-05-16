package cuchaz.enigma.source.procyon.typeloader;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

/**
 * Typeloader with synchronized tryLoadType method
 */
public class SynchronizedTypeLoader implements ITypeLoader {
	private final ITypeLoader delegate;

	public SynchronizedTypeLoader(ITypeLoader delegate) {
		this.delegate = delegate;
	}

	@Override
	public synchronized boolean tryLoadType(String internalName, Buffer buffer) {
		return delegate.tryLoadType(internalName, buffer);
	}
}
