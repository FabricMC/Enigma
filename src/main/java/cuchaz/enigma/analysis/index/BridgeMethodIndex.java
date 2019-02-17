package cuchaz.enigma.analysis.index;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BridgeMethodIndex implements JarIndexer {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;

	private final Set<MethodEntry> bridgeMethods = Sets.newHashSet();
	private final Map<MethodEntry, MethodEntry> accessedToBridge = Maps.newHashMap();

	public BridgeMethodIndex(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
	}

	@Override
	public void processIndex(EntryResolver resolver) {
		// look for access and bridged methods
		for (MethodEntry methodEntry : entryIndex.getMethods()) {
			if (methodEntry.getParent().getName().equals("fn") && (methodEntry.getName().equals("compareTo") || methodEntry.getName().equals("l"))) {
				System.out.println();
			}

			AccessFlags access = entryIndex.getMethodAccess(methodEntry);
			if (access == null || !access.isSynthetic()) {
				continue;
			}

			indexSyntheticMethod((MethodDefEntry) methodEntry, access);
		}
	}

	private void indexSyntheticMethod(MethodDefEntry syntheticMethod, AccessFlags access) {
		MethodEntry accessedMethod = findAccessMethod(syntheticMethod);
		if (accessedMethod == null) {
			return;
		}

		if (access.isBridge() || isPotentialBridge(syntheticMethod, accessedMethod)) {
			bridgeMethods.add(syntheticMethod);
			accessedToBridge.put(accessedMethod, syntheticMethod);
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

	private boolean isPotentialBridge(MethodDefEntry bridgeMethod, MethodEntry accessedMethod) {
		// Bridge methods only exist for inheritance purposes, if we're private, final, or static, we cannot be inherited
		AccessFlags bridgeAccess = bridgeMethod.getAccess();
		if (bridgeAccess.isPrivate() || bridgeAccess.isFinal() || bridgeAccess.isStatic()) {
			return false;
		}

		MethodDescriptor bridgeDesc = bridgeMethod.getDesc();
		MethodDescriptor accessedDesc = accessedMethod.getDesc();
		List<TypeDescriptor> bridgeArguments = bridgeDesc.getArgumentDescs();
		List<TypeDescriptor> accessedArguments = accessedDesc.getArgumentDescs();

		// A bridge method will always have the same number of arguments
		if (bridgeArguments.size() != accessedArguments.size()) {
			return false;
		}

		// Check that all argument types are bridge-compatible
		for (int i = 0; i < bridgeArguments.size(); i++) {
			if (!areTypesBridgeCompatible(bridgeArguments.get(i), accessedArguments.get(i))) {
				return false;
			}
		}

		// Check that the return type is bridge-compatible
		return areTypesBridgeCompatible(bridgeDesc.getReturnDesc(), accessedDesc.getReturnDesc());
	}

	private boolean areTypesBridgeCompatible(TypeDescriptor bridgeDesc, TypeDescriptor accessedDesc) {
		if (bridgeDesc.equals(accessedDesc)) {
			return true;
		}

		// Either the descs will be equal, or they are both types and different through a generic
		if (bridgeDesc.isType() && accessedDesc.isType()) {
			ClassEntry bridgeType = bridgeDesc.getTypeEntry();
			ClassEntry accessedType = accessedDesc.getTypeEntry();

			// If the given types are completely unrelated to each other, this can't be bridge compatible
			InheritanceIndex.Relation relation = inheritanceIndex.computeClassRelation(accessedType, bridgeType);
			return relation != InheritanceIndex.Relation.UNRELATED;
		}

		return false;
	}

	public boolean isBridgeMethod(MethodEntry entry) {
		return bridgeMethods.contains(entry);
	}

	public boolean isAccessedByBridge(MethodEntry entry) {
		return accessedToBridge.containsKey(entry);
	}

	@Nullable
	public MethodEntry getBridgeFromAccessed(MethodEntry entry) {
		return accessedToBridge.get(entry);
	}
}
