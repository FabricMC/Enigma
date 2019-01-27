package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;

public class EntryRemapper {
	private final DeltaTrackingTree<EntryMapping> obfToDeobf;

	private final EntryResolver obfResolver;
	private final Translator deobfuscator;

	private final MappingValidator validator;

	public EntryRemapper(JarIndex jarIndex, EntryTree<EntryMapping> obfToDeobf) {
		this.obfToDeobf = new DeltaTrackingTree<>(obfToDeobf);

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, obfResolver);

		this.validator = new MappingValidator(obfToDeobf, deobfuscator, obfResolver);
	}

	public EntryRemapper(JarIndex jarIndex) {
		this(jarIndex, new HashEntryTree<>());
	}

	public <E extends Entry<?>> void mapFromObf(E obfuscatedEntry, @Nullable EntryMapping deobfMapping) {
		Collection<E> resolvedEntries = obfResolver.resolveEntry(obfuscatedEntry, ResolutionStrategy.RESOLVE_ROOT);
		for (E resolvedEntry : resolvedEntries) {
			if (deobfMapping != null) {
				validator.validateRename(resolvedEntry, deobfMapping.getTargetName());
			}

			obfToDeobf.insert(obfuscatedEntry, deobfMapping);
		}
	}

	public void removeByObf(Entry<?> obfuscatedEntry) {
		mapFromObf(obfuscatedEntry, null);
	}

	@Nullable
	public EntryMapping getDeobfMapping(Entry<?> entry) {
		return obfToDeobf.get(entry);
	}

	public boolean hasDeobfMapping(Entry<?> obfEntry) {
		return obfToDeobf.contains(obfEntry);
	}

	public <T extends Translatable> T deobfuscate(T translatable) {
		return deobfuscator.translate(translatable);
	}

	public Translator getDeobfuscator() {
		return deobfuscator;
	}

	public Collection<Entry<?>> getObfEntries() {
		return obfToDeobf.getAllEntries();
	}

	public Collection<Entry<?>> getObfRootEntries() {
		return obfToDeobf.getRootEntries();
	}

	public Collection<Entry<?>> getObfChildren(Entry<?> obfuscatedEntry) {
		return obfToDeobf.getChildren(obfuscatedEntry);
	}

	public DeltaTrackingTree<EntryMapping> getObfToDeobf() {
		return obfToDeobf;
	}

	public MappingDelta<EntryMapping> takeMappingDelta() {
		return obfToDeobf.takeDelta();
	}

	public boolean isDirty() {
		return obfToDeobf.isDirty();
	}

	public EntryResolver getObfResolver() {
		return obfResolver;
	}
}
