package cuchaz.enigma.analysis.index;

import com.google.common.collect.Maps;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class BridgeMethodIndex implements JarIndexer, RemappableIndex {
	private final EntryIndex entryIndex;
	private final ReferenceIndex referenceIndex;

	private Map<MethodEntry, MethodEntry> bridgeMethods = Maps.newHashMap();
	private Map<MethodEntry, MethodEntry> bridgedMethods = Maps.newHashMap();

	public BridgeMethodIndex(EntryIndex entryIndex, ReferenceIndex referenceIndex) {
		this.entryIndex = entryIndex;
		this.referenceIndex = referenceIndex;
	}

	@Override
	public void remap(Translator translator) {
		bridgeMethods = translator.translate(bridgeMethods);
		bridgedMethods = translator.translate(bridgedMethods);
	}

	@Override
	public BridgeMethodIndex remapped(Translator translator) {
		BridgeMethodIndex index = new BridgeMethodIndex(entryIndex, referenceIndex);
		index.bridgeMethods = translator.translate(bridgeMethods);
		index.bridgedMethods = translator.translate(bridgedMethods);

		return index;
	}

	@Override
	public void processIndex() {
		// look for access and bridged methods
		for (MethodEntry methodEntry : entryIndex.getMethods()) {
			AccessFlags access = entryIndex.getMethodAccess(methodEntry);
			if (access == null || !access.isSynthetic()) {
				continue;
			}

			indexSyntheticMethod(methodEntry, access);
		}
	}

	private void indexSyntheticMethod(MethodEntry methodEntry, AccessFlags access) {
		if (access.isBridge()) {
			MethodEntry accessedMethod = findAccessMethod(methodEntry);
			if (accessedMethod != null) {
				bridgeMethods.put(methodEntry, accessedMethod);
				bridgedMethods.put(accessedMethod, methodEntry);
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
	public MethodEntry getBridgedMethod(MethodEntry entry) {
		return bridgeMethods.get(entry);
	}

	@Nullable
	public MethodEntry getBridgeMethod(MethodEntry entry) {
		return bridgedMethods.get(entry);
	}
}
