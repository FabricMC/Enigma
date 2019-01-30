package cuchaz.enigma.analysis.index;

import com.google.common.collect.Maps;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class BridgeMethodIndex implements JarIndexer {
	private final EntryIndex entryIndex;
	private final ReferenceIndex referenceIndex;

	private Map<MethodEntry, MethodEntry> accessedToBridge = Maps.newHashMap();

	public BridgeMethodIndex(EntryIndex entryIndex, ReferenceIndex referenceIndex) {
		this.entryIndex = entryIndex;
		this.referenceIndex = referenceIndex;
	}

	@Override
	public void processIndex(EntryResolver resolver) {
		// look for access and bridged methods
		for (MethodEntry methodEntry : entryIndex.getMethods()) {
			AccessFlags access = entryIndex.getMethodAccess(methodEntry);
			if (access == null || !access.isSynthetic()) {
				continue;
			}

			indexSyntheticMethod(methodEntry, access);
		}
	}

	private void indexSyntheticMethod(MethodEntry syntheticMethod, AccessFlags access) {
		if (access.isBridge()) {
			MethodEntry accessedMethod = findAccessMethod(syntheticMethod);
			if (accessedMethod != null) {
				accessedToBridge.put(accessedMethod, syntheticMethod);
			}
		}
	}

	private MethodEntry findAccessMethod(MethodEntry method) {
		// we want to find all compiler-added methods that directly call another with no processing

		// get all the methods that we call
		final Collection<MethodEntry> referencedMethods = referenceIndex.getMethodsReferencedBy(method);

		// is there just one?
		if (referencedMethods.size() != 1) {
			return null;
		}

		return referencedMethods.stream().findFirst().orElse(null);
	}

	@Nullable
	public MethodEntry getBridgeFromAccessed(MethodEntry entry) {
		return accessedToBridge.get(entry);
	}
}
