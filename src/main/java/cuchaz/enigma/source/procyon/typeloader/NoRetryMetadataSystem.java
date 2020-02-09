package cuchaz.enigma.source.procyon.typeloader;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NoRetryMetadataSystem extends MetadataSystem {
	private final Set<String> failedTypes = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public NoRetryMetadataSystem(final ITypeLoader typeLoader) {
		super(typeLoader);
	}

	@Override
	protected synchronized TypeDefinition resolveType(final String descriptor, final boolean mightBePrimitive) {
		if (failedTypes.contains(descriptor)) {
			return null;
		}

		final TypeDefinition result = super.resolveType(descriptor, mightBePrimitive);

		if (result == null) {
			failedTypes.add(descriptor);
		}

		return result;
	}

	@Override
	public synchronized TypeDefinition resolve(final TypeReference type) {
		return super.resolve(type);
	}
}
