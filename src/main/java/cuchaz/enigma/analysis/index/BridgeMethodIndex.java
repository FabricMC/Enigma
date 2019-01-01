package cuchaz.enigma.analysis.index;

import com.google.common.collect.*;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class BridgeMethodIndex implements JarIndexer, RemappableIndex {
	private final EntryIndex entryIndex;
	private final ReferenceIndex referenceIndex;

	private final Map<MethodEntry, MethodEntry> bridgedMethods = Maps.newHashMap();
	private final Multimap<MethodEntry, MethodEntry> inverseBridges = HashMultimap.create();

	public BridgeMethodIndex(EntryIndex entryIndex, ReferenceIndex referenceIndex) {
		this.entryIndex = entryIndex;
		this.referenceIndex = referenceIndex;
	}

	@Override
	public BridgeMethodIndex remapped(Translator translator) {
		BridgeMethodIndex remapped = new BridgeMethodIndex(entryIndex, referenceIndex);
		for (Map.Entry<MethodEntry, MethodEntry> entry : bridgedMethods.entrySet()) {
			MethodEntry key = translator.translate(entry.getKey());
			MethodEntry value = translator.translate(entry.getValue());
			remapped.bridgedMethods.put(key, value);
			remapped.inverseBridges.put(value, key);
		}

		return remapped;
	}

	@Override
	public void remapEntry(Entry<?> entry, Entry<?> newEntry) {
		if (entry instanceof MethodEntry) {
			MethodEntry bridgedMethod = bridgedMethods.remove(entry);
			bridgedMethods.put((MethodEntry) newEntry, bridgedMethod);

			Collection<MethodEntry> accessorEntries = inverseBridges.removeAll(entry);
			for (MethodEntry accessorEntry : accessorEntries) {
				bridgedMethods.put(accessorEntry, (MethodEntry) newEntry);
			}

			inverseBridges.putAll((MethodEntry) newEntry, accessorEntries);
		}
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
				bridgedMethods.put(methodEntry, accessedMethod);
				inverseBridges.put(accessedMethod, methodEntry);
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
		return bridgedMethods.get(entry);
	}
}
