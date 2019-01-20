package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;

public class EntryRemapper {
	private final EntryTree<EntryMapping> obfToDeobf;
	private final DeltaTrackingTree<EntryMapping> deobfToObf;

	private final JarIndex obfIndex;
	private JarIndex deobfIndex;

	private final EntryResolver obfResolver;
	private EntryResolver deobfResolver;

	private final Translator deobfuscator;
	private Translator obfuscator;

	private final MappingValidator validator;

	private EntryRemapper(JarIndex jarIndex, EntryTree<EntryMapping> obfToDeobf, EntryTree<EntryMapping> deobfToObf) {
		this.obfToDeobf = obfToDeobf;
		this.deobfToObf = new DeltaTrackingTree<>(deobfToObf);

		this.obfIndex = jarIndex;
		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, obfResolver);
		rebuildDeobfIndex();

		this.validator = new MappingValidator(obfToDeobf, obfResolver);
	}

	public EntryRemapper(JarIndex jarIndex) {
		this(jarIndex, new HashEntryTree<>(), new HashEntryTree<>());
	}

	public EntryRemapper(JarIndex jarIndex, EntryTree<EntryMapping> deobfuscationTrees) {
		this(jarIndex, deobfuscationTrees, inverse(deobfuscationTrees));
	}

	private static EntryTree<EntryMapping> inverse(EntryTree<EntryMapping> tree) {
		Translator translator = new MappingTranslator(tree, VoidEntryResolver.INSTANCE);
		EntryTree<EntryMapping> inverse = new HashEntryTree<>();

		// Naive approach, could operate on the nodes of the tree. However, this runs infrequently.
		Collection<Entry<?>> entries = tree.getAllEntries();
		for (Entry<?> sourceEntry : entries) {
			Entry<?> targetEntry = translator.translate(sourceEntry);
			inverse.insert(targetEntry, new EntryMapping(sourceEntry.getName()));
		}

		return inverse;
	}

	private void rebuildDeobfIndex() {
		this.deobfIndex = obfIndex.remapped(deobfuscator);

		this.deobfResolver = deobfIndex.getEntryResolver();
		this.obfuscator = new MappingTranslator(deobfToObf, deobfResolver);
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

		// Temporary hack, not very performant
		rebuildDeobfIndex();

		// Reconstruct the children of this node in the deobf -> obf tree with our new mapping
		// We only need to do this for deobf -> obf because the obf tree is always consistent on the left hand side
		// We lookup by obf, and the obf never changes. This is not the case for deobf so we need to update the tree.

		EntryTreeNode<EntryMapping> node = deobfToObf.findNode(prevDeobf);
		if (node != null) {
			for (EntryTreeNode<EntryMapping> child : node.getNodesRecursively()) {
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
		return obfToDeobf.get(entry);
	}

	@Nullable
	public EntryMapping getObfMapping(Entry<?> entry) {
		return deobfToObf.get(entry);
	}

	public boolean hasDeobfMapping(Entry<?> obfEntry) {
		return obfToDeobf.contains(obfEntry);
	}

	public boolean hasObfMapping(Entry<?> deobfEntry) {
		return deobfToObf.contains(deobfEntry);
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

	public EntryTree<EntryMapping> getObfToDeobf() {
		return obfToDeobf;
	}

	public DeltaTrackingTree<EntryMapping> getDeobfToObf() {
		return deobfToObf;
	}

	public MappingDelta takeMappingDelta() {
		MappingDelta delta = deobfToObf.takeDelta();
		return delta.translate(obfuscator, VoidEntryResolver.INSTANCE, deobfToObf);
	}

	public boolean isDirty() {
		return deobfToObf.isDirty();
	}

	public EntryResolver getObfResolver() {
		return obfResolver;
	}

	public EntryResolver getDeobfResolver() {
		return deobfResolver;
	}
}
