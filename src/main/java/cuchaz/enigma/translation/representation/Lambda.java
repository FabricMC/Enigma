package cuchaz.enigma.translation.representation;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;

import java.util.Objects;

public class Lambda implements Translatable {
	private final String invokedName;
	private final MethodDescriptor invokedType;
	private final MethodDescriptor samMethodType;
	private final ParentedEntry<?> implMethod;
	private final MethodDescriptor instantiatedMethodType;

	public Lambda(String invokedName, MethodDescriptor invokedType, MethodDescriptor samMethodType, ParentedEntry<?> implMethod, MethodDescriptor instantiatedMethodType) {
		this.invokedName = invokedName;
		this.invokedType = invokedType;
		this.samMethodType = samMethodType;
		this.implMethod = implMethod;
		this.instantiatedMethodType = instantiatedMethodType;
	}

	@Override
	public Lambda translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		MethodEntry samMethod = new MethodEntry(getInterface(), invokedName, samMethodType);
		EntryMapping samMethodMapping = resolveMapping(resolver, mappings, samMethod);

		return new Lambda(
			samMethodMapping != null ? samMethodMapping.getTargetName() : invokedName,
			invokedType.translate(translator, resolver, mappings),
			samMethodType.translate(translator, resolver, mappings),
			implMethod.translate(translator, resolver, mappings),
			instantiatedMethodType.translate(translator, resolver, mappings)
		);
	}

	private EntryMapping resolveMapping(EntryResolver resolver, EntryMap<EntryMapping> mappings, MethodEntry methodEntry) {
		for (MethodEntry entry : resolver.resolveEntry(methodEntry, ResolutionStrategy.RESOLVE_ROOT)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				return mapping;
			}
		}
		return null;
	}

	public ClassEntry getInterface() {
		return invokedType.getReturnDesc().getTypeEntry();
	}

	public String getInvokedName() {
		return invokedName;
	}

	public MethodDescriptor getInvokedType() {
		return invokedType;
	}

	public MethodDescriptor getSamMethodType() {
		return samMethodType;
	}

	public ParentedEntry<?> getImplMethod() {
		return implMethod;
	}

	public MethodDescriptor getInstantiatedMethodType() {
		return instantiatedMethodType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Lambda lambda = (Lambda) o;
		return Objects.equals(invokedName, lambda.invokedName) &&
			Objects.equals(invokedType, lambda.invokedType) &&
			Objects.equals(samMethodType, lambda.samMethodType) &&
			Objects.equals(implMethod, lambda.implMethod) &&
			Objects.equals(instantiatedMethodType, lambda.instantiatedMethodType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(invokedName, invokedType, samMethodType, implMethod, instantiatedMethodType);
	}

	@Override
	public String toString() {
		return "Lambda{" +
			"invokedName='" + invokedName + '\'' +
			", invokedType=" + invokedType +
			", samMethodType=" + samMethodType +
			", implMethod=" + implMethod +
			", instantiatedMethodType=" + instantiatedMethodType +
			'}';
	}
}
