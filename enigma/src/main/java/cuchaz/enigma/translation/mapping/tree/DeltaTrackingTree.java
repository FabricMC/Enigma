package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public class DeltaTrackingTree<T> implements EntryTree<T> {
	private final EntryTree<T> delegate;

	private EntryTree<T> deltaReference;
	private EntryTree<Object> changes = new HashEntryTree<>();

	public DeltaTrackingTree(EntryTree<T> delegate) {
		this.delegate = delegate;
		this.deltaReference = new HashEntryTree<>(delegate);
	}

	public DeltaTrackingTree() {
		this(new HashEntryTree<>());
	}

	@Override
	public void insert(Entry<?> entry, T value) {
		trackChange(entry);
		delegate.insert(entry, value);
	}

	@Nullable
	@Override
	public T remove(Entry<?> entry) {
		trackChange(entry);
		return delegate.remove(entry);
	}

	public void trackChange(Entry<?> entry) {
		changes.insert(entry, MappingDelta.PLACEHOLDER);
	}

	@Nullable
	@Override
	public T get(Entry<?> entry) {
		return delegate.get(entry);
	}

	@Override
	public Collection<Entry<?>> getChildren(Entry<?> entry) {
		return delegate.getChildren(entry);
	}

	@Override
	public Collection<Entry<?>> getSiblings(Entry<?> entry) {
		return delegate.getSiblings(entry);
	}

	@Nullable
	@Override
	public EntryTreeNode<T> findNode(Entry<?> entry) {
		return delegate.findNode(entry);
	}

	@Override
	public Stream<EntryTreeNode<T>> getRootNodes() {
		return delegate.getRootNodes();
	}

	@Override
	public DeltaTrackingTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		DeltaTrackingTree<T> translatedTree = new DeltaTrackingTree<>(delegate.translate(translator, resolver, mappings));
		translatedTree.changes = changes.translate(translator, resolver, mappings);
		return translatedTree;
	}

	@Override
	public Stream<Entry<?>> getAllEntries() {
		return delegate.getAllEntries();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public Iterator<EntryTreeNode<T>> iterator() {
		return delegate.iterator();
	}

	public MappingDelta<T> takeDelta() {
		MappingDelta<T> delta = new MappingDelta<>(deltaReference, changes);
		resetDelta();
		return delta;
	}

	private void resetDelta() {
		deltaReference = new HashEntryTree<>(delegate);
		changes = new HashEntryTree<>();
	}

	public boolean isDirty() {
		return !changes.isEmpty();
	}
}
