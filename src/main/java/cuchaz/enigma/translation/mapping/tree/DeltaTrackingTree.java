package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

public class DeltaTrackingTree<M> implements MappingTree<M> {
	private final MappingTree<M> delegate;

	private MappingTree<Object> additions = new HashMappingTree<>();
	private MappingTree<Object> deletions = new HashMappingTree<>();

	public DeltaTrackingTree(MappingTree<M> delegate) {
		this.delegate = delegate;
	}

	public DeltaTrackingTree() {
		this(new HashMappingTree<>());
	}

	@Override
	public void insert(Entry<?> entry, M mapping) {
		if (mapping != null) {
			trackAddition(entry);
		} else {
			trackDeletion(entry);
		}
		delegate.insert(entry, mapping);
	}

	@Override
	public void remove(Entry<?> entry) {
		delegate.remove(entry);
		trackDeletion(entry);
	}

	private void trackAddition(Entry<?> entry) {
		deletions.remove(entry);
		additions.insert(entry, MappingDelta.PLACEHOLDER);
	}

	private void trackDeletion(Entry<?> entry) {
		additions.remove(entry);
		deletions.insert(entry, MappingDelta.PLACEHOLDER);
	}

	@Nullable
	@Override
	public M getMapping(Entry<?> entry) {
		return delegate.getMapping(entry);
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
	public MappingNode<M> findNode(Entry<?> entry) {
		return delegate.findNode(entry);
	}

	@Override
	public Collection<Entry<?>> getRootEntries() {
		return delegate.getRootEntries();
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
	public Iterator<MappingNode<M>> iterator() {
		return delegate.iterator();
	}

	public MappingDelta takeDelta() {
		MappingDelta delta = new MappingDelta(additions, deletions);
		resetDelta();
		return delta;
	}

	private void resetDelta() {
		additions = new HashMappingTree<>();
		deletions = new HashMappingTree<>();
	}

	public boolean isDirty() {
		return !additions.isEmpty() || !deletions.isEmpty();
	}
}
