package cuchaz.enigma.analysis.index;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.*;

public class BridgeMethodIndex implements JarIndexer {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;

	private final Set<MethodEntry> bridgeToSpecialized = Sets.newHashSet();
	private final Map<MethodEntry, MethodEntry> specializedToBridge = Maps.newHashMap();

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

		for (Map.Entry<MethodEntry, MethodEntry> entry : copiedAccessToBridge.entrySet()) {
			MethodEntry specializedEntry = entry.getKey();
			MethodEntry bridgeEntry = entry.getValue();
			if (bridgeEntry.getName().equals(specializedEntry.getName())) {
				continue;
			}

			MethodEntry renamedSpecializedEntry = specializedEntry.withName(bridgeEntry.getName());
			bridgeToSpecialized.add(renamedSpecializedEntry);
			specializedToBridge.put(renamedSpecializedEntry, specializedToBridge.get(specializedEntry));
		}
	}

	private void indexSyntheticMethod(MethodDefEntry syntheticMethod, AccessFlags access) {
		MethodEntry specializedMethod = findSpecializedMethod(syntheticMethod);
		if (specializedMethod == null) {
			return;
		}

		if (access.isBridge() || isPotentialBridge(syntheticMethod, specializedMethod)) {
			bridgeToSpecialized.add(syntheticMethod);
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
		return bridgeToSpecialized.contains(entry);
	}

	public boolean isSpecializedMethod(MethodEntry entry) {
		return specializedToBridge.containsKey(entry);
	}

	@Nullable
	public MethodEntry getBridgeFromSpecialized(MethodEntry specialized) {
		return specializedToBridge.get(specialized);
	}

	public Map<MethodEntry, MethodEntry> getSpecializedToBridge() {
		return Collections.unmodifiableMap(specializedToBridge);
	}
}
