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

public class DeltaTrackingTree<T> implements EntryTree<T> {
	private final EntryTree<T> delegate;

	private EntryTree<T> deltaReference;
	private EntryTree<Object> additions = new HashEntryTree<>();
	private EntryTree<Object> deletions = new HashEntryTree<>();

	public DeltaTrackingTree(EntryTree<T> delegate) {
		this.delegate = delegate;
		this.deltaReference = new HashEntryTree<>(delegate);
	}

	public DeltaTrackingTree() {
		this(new HashEntryTree<>());
	}

	@Override
	public void insert(Entry<?> entry, T value) {
		if (value != null) {
			trackAddition(entry);
		} else {
			trackDeletion(entry);
		}
		delegate.insert(entry, value);
	}

	@Nullable
	@Override
	public T remove(Entry<?> entry) {
		T value = delegate.remove(entry);
		trackDeletion(entry);
		return value;
	}

	public void trackAddition(Entry<?> entry) {
		deletions.insert(entry, MappingDelta.PLACEHOLDER);
		additions.insert(entry, MappingDelta.PLACEHOLDER);
	}

	public void trackDeletion(Entry<?> entry) {
		additions.remove(entry);
		deletions.insert(entry, MappingDelta.PLACEHOLDER);
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
	public Collection<EntryTreeNode<T>> getAllNodes() {
		return delegate.getAllNodes();
	}

	@Override
	public Collection<Entry<?>> getRootEntries() {
		return delegate.getRootEntries();
	}

	@Override
	public DeltaTrackingTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		DeltaTrackingTree<T> translatedTree = new DeltaTrackingTree<>(delegate.translate(translator, resolver, mappings));
		translatedTree.additions = additions.translate(translator, resolver, mappings);
		translatedTree.deletions = deletions.translate(translator, resolver, mappings);
		return translatedTree;
	}

	@Override
	public Collection<Entry<?>> getAllEntries() {
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
		MappingDelta<T> delta = new MappingDelta<>(deltaReference, additions, deletions);
		resetDelta();
		return delta;
	}

	private void resetDelta() {
		deltaReference = new HashEntryTree<>(delegate);
		additions = new HashEntryTree<>();
		deletions = new HashEntryTree<>();
	}

	public boolean isDirty() {
		return !additions.isEmpty() || !deletions.isEmpty();
	}
}
