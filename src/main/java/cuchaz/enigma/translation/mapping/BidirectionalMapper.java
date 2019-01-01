package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.HashMappingTree;
import cuchaz.enigma.translation.mapping.tree.MappingNode;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;

public class BidirectionalMapper {
	private final MappingTree<EntryMapping> obfToDeobf;
	private final DeltaTrackingTree<EntryMapping> deobfToObf;

	private final JarIndex deobfIndex;

	private final EntryResolver obfResolver;

	private final Translator deobfuscator;
	private final Translator obfuscator;

	private final MappingValidator validator;

	private BidirectionalMapper(JarIndex jarIndex, MappingTree<EntryMapping> obfToDeobf, MappingTree<EntryMapping> deobfToObf) {
		this.obfToDeobf = obfToDeobf;
		this.deobfToObf = new DeltaTrackingTree<>(deobfToObf);

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, obfResolver);
		this.deobfIndex = jarIndex.remapped(this.deobfuscator);

		EntryResolver deobfResolver = deobfIndex.getEntryResolver();
		this.obfuscator = new MappingTranslator(deobfToObf, deobfResolver);

		this.validator = new MappingValidator(obfToDeobf, obfResolver);
	}

	public BidirectionalMapper(JarIndex jarIndex) {
		this(jarIndex, new HashMappingTree<>(), new HashMappingTree<>());
	}

	public BidirectionalMapper(JarIndex jarIndex, MappingTree<EntryMapping> deobfuscationTrees) {
		this(jarIndex, deobfuscationTrees, inverse(deobfuscationTrees));
	}

	private static MappingTree<EntryMapping> inverse(MappingTree<EntryMapping> tree) {
		Translator translator = new MappingTranslator(tree, VoidEntryResolver.INSTANCE);
		MappingTree<EntryMapping> inverse = new HashMappingTree<>();

		// Naive approach, could operate on the nodes of the tree. However, this runs infrequently.
		Collection<Entry<?>> entries = tree.getAllEntries();
		for (Entry<?> sourceEntry : entries) {
			Entry<?> targetEntry = translator.translate(sourceEntry);
			inverse.insert(targetEntry, new EntryMapping(sourceEntry.getName()));
		}

		return inverse;
	}

	public <E extends Entry<?>> void mapFromObf(E obfuscatedEntry, @Nullable EntryMapping deobfMapping) {
		E resolvedEntry = obfResolver.resolveEntry(obfuscatedEntry);

		if (deobfMapping != null) {
			validator.validateRename(resolvedEntry, deobfMapping.getTargetName());
		}

		setObfToDeobf(resolvedEntry, deobfMapping);
	}

	public <E extends Entry<?>> void mapFromDeobf(E deobfuscatedEntry, @Nullable EntryMapping deobfMapping) {
		E obfuscatedEntry = obfuscate(deobfuscatedEntry);
		mapFromObf(obfuscatedEntry, deobfMapping);
	}

	public void removeByObf(Entry<?> obfuscatedEntry) {
		mapFromObf(obfuscatedEntry, null);
	}

	public void removeByDeobf(Entry<?> deobfuscatedEntry) {
		mapFromObf(obfuscate(deobfuscatedEntry), null);
	}

	private <E extends Entry<?>> void setObfToDeobf(E obfuscatedEntry, @Nullable EntryMapping deobfMapping) {
		E prevDeobf = deobfuscate(obfuscatedEntry);
		obfToDeobf.insert(obfuscatedEntry, deobfMapping);

		E newDeobf = deobfuscate(obfuscatedEntry);

		deobfIndex.remapEntry(prevDeobf, newDeobf);

		// Reconstruct the children of this node in the deobf -> obf tree with our new mapping
		// We only need to do this for deobf -> obf because the obf tree is always consistent on the left hand side
		// We lookup by obf, and the obf never changes. This is not the case for deobf so we need to update the tree.

		MappingNode<EntryMapping> node = deobfToObf.findNode(prevDeobf);
		if (node != null) {
			for (MappingNode<EntryMapping> child : node.getNodesRecursively()) {
				Entry<?> entry = child.getEntry();
				EntryMapping mapping = new EntryMapping(obfuscate(entry).getName());

				deobfToObf.insert(entry.replaceAncestor(prevDeobf, newDeobf), mapping);
				deobfToObf.remove(entry);
			}
		} else {
			deobfToObf.insert(newDeobf, new EntryMapping(obfuscatedEntry.getName()));
		}
	}

	@Nullable
	public EntryMapping getDeobfMapping(Entry<?> entry) {
		return obfToDeobf.getMapping(entry);
	}

	@Nullable
	public EntryMapping getObfMapping(Entry<?> entry) {
		return deobfToObf.getMapping(entry);
	}

	public boolean hasDeobfMapping(Entry<?> obfEntry) {
		return obfToDeobf.hasMapping(obfEntry);
	}

	public boolean hasObfMapping(Entry<?> deobfEntry) {
		return deobfToObf.hasMapping(deobfEntry);
	}

	public <T extends Translatable> T deobfuscate(T translatable) {
		return deobfuscator.translate(translatable);
	}

	public <T extends Translatable> T obfuscate(T translatable) {
		return obfuscator.translate(translatable);
	}

	public Translator getDeobfuscator() {
		return deobfuscator;
	}

	public Translator getObfuscator() {
		return obfuscator;
	}

	public Collection<Entry<?>> getObfEntries() {
		return obfToDeobf.getAllEntries();
	}

	public Collection<Entry<?>> getObfRootEntries() {
		return obfToDeobf.getRootEntries();
	}

	public Collection<Entry<?>> getDeobfEntries() {
		return deobfToObf.getAllEntries();
	}

	public Collection<Entry<?>> getObfChildren(Entry<?> obfuscatedEntry) {
		return obfToDeobf.getChildren(obfuscatedEntry);
	}

	public Collection<Entry<?>> getDeobfChildren(Entry<?> deobfuscatedEntry) {
		return deobfToObf.getChildren(deobfuscatedEntry);
	}

	public MappingTree<EntryMapping> getObfToDeobf() {
		return obfToDeobf;
	}

	public MappingTree<EntryMapping> getDeobfToObf() {
		return deobfToObf;
	}

	public MappingDelta takeMappingDelta() {
		MappingDelta delta = deobfToObf.takeDelta();
		return delta.translate(obfuscator, VoidEntryResolver.INSTANCE, deobfToObf);
	}

	public boolean isDirty() {
		return deobfToObf.isDirty();
	}
}
