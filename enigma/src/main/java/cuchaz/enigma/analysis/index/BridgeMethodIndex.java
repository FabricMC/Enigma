package cuchaz.enigma.analysis.index;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class BridgeMethodIndex implements JarIndexer {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;

	private final ConcurrentMap<MethodEntry, MethodEntry> bridgeToSpecialized = new ConcurrentHashMap<>();
	private final ConcurrentMap<MethodEntry, MethodEntry> specializedToBridge = new ConcurrentHashMap<>();

	public BridgeMethodIndex(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
	}

	public void findBridgeMethods() {
		// look for access and bridged methods
		for (MethodEntry methodEntry : entryIndex.getMethods()) {
			MethodDefEntry methodDefEntry = (MethodDefEntry) methodEntry;

			AccessFlags access = methodDefEntry.getAccess();

			if (access == null || !access.isSynthetic()) {
				continue;
			}

			indexSyntheticMethod(methodDefEntry, access);
		}
	}

	@Override
	public void processIndex(JarIndex index) {
		Map<MethodEntry, MethodEntry> copiedAccessToBridge = new HashMap<>(specializedToBridge);

		copiedAccessToBridge.entrySet().parallelStream().forEach(entry -> {
			MethodEntry specializedEntry = entry.getKey();
			MethodEntry bridgeEntry = entry.getValue();

			if (bridgeEntry.getName().equals(specializedEntry.getName())) {
				return;
			}

			MethodEntry renamedSpecializedEntry = specializedEntry.withName(bridgeEntry.getName());
			specializedToBridge.put(renamedSpecializedEntry, copiedAccessToBridge.get(specializedEntry));
		});
	}

	private void indexSyntheticMethod(MethodDefEntry syntheticMethod, AccessFlags access) {
		MethodEntry specializedMethod = findSpecializedMethod(syntheticMethod);

		if (specializedMethod == null) {
			return;
		}

		if (access.isBridge() || isPotentialBridge(syntheticMethod, specializedMethod)) {
			bridgeToSpecialized.put(syntheticMethod, specializedMethod);
			specializedToBridge.put(specializedMethod, syntheticMethod);
		}
	}

	private MethodEntry findSpecializedMethod(MethodEntry method) {
		// we want to find all compiler-added methods that directly call another with no processing

		// get all the methods that we call
		final Collection<MethodEntry> referencedMethods = referenceIndex.getMethodsReferencedBy(method);

		// is there just one?
		if (referencedMethods.size() != 1) {
			return null;
		}

		return referencedMethods.stream().findFirst().orElse(null);
	}

	private boolean isPotentialBridge(MethodDefEntry bridgeMethod, MethodEntry specializedMethod) {
		// Bridge methods only exist for inheritance purposes, if we're private, final, or static, we cannot be inherited
		AccessFlags bridgeAccess = bridgeMethod.getAccess();

		if (bridgeAccess.isPrivate() || bridgeAccess.isFinal() || bridgeAccess.isStatic()) {
			return false;
		}

		MethodDescriptor bridgeDesc = bridgeMethod.getDesc();
		MethodDescriptor specializedDesc = specializedMethod.getDesc();
		List<TypeDescriptor> bridgeArguments = bridgeDesc.getArgumentDescs();
		List<TypeDescriptor> specializedArguments = specializedDesc.getArgumentDescs();

		// A bridge method will always have the same number of arguments
		if (bridgeArguments.size() != specializedArguments.size()) {
			return false;
		}

		// Check that all argument types are bridge-compatible
		for (int i = 0; i < bridgeArguments.size(); i++) {
			if (!areTypesBridgeCompatible(bridgeArguments.get(i), specializedArguments.get(i))) {
				return false;
			}
		}

		// Check that the return type is bridge-compatible
		return areTypesBridgeCompatible(bridgeDesc.getReturnDesc(), specializedDesc.getReturnDesc());
	}

	private boolean areTypesBridgeCompatible(TypeDescriptor bridgeDesc, TypeDescriptor specializedDesc) {
		if (bridgeDesc.equals(specializedDesc)) {
			return true;
		}

		// Either the descs will be equal, or they are both types and different through a generic
		if (bridgeDesc.isType() && specializedDesc.isType()) {
			ClassEntry bridgeType = bridgeDesc.getTypeEntry();
			ClassEntry accessedType = specializedDesc.getTypeEntry();

			// If the given types are completely unrelated to each other, this can't be bridge compatible
			InheritanceIndex.Relation relation = inheritanceIndex.computeClassRelation(accessedType, bridgeType);
			return relation != InheritanceIndex.Relation.UNRELATED;
		}

		return false;
	}

	public boolean isBridgeMethod(MethodEntry entry) {
		return bridgeToSpecialized.containsKey(entry);
	}

	public boolean isSpecializedMethod(MethodEntry entry) {
		return specializedToBridge.containsKey(entry);
	}

	@Nullable
	public MethodEntry getBridgeFromSpecialized(MethodEntry specialized) {
		return specializedToBridge.get(specialized);
	}

	public MethodEntry getSpecializedFromBridge(MethodEntry bridge) {
		return bridgeToSpecialized.get(bridge);
	}

	/** Includes "renamed specialized -> bridge" entries. */
	public Map<MethodEntry, MethodEntry> getSpecializedToBridge() {
		return Collections.unmodifiableMap(specializedToBridge);
	}

	/** Only "bridge -> original name" entries. **/
	public Map<MethodEntry, MethodEntry> getBridgeToSpecialized() {
		return Collections.unmodifiableMap(bridgeToSpecialized);
	}
}
