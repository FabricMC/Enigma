package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

public class DeltaTrackingTree<M> implements MappingTree<M> {
	private final MappingTree<M> delegate;

	private MappingTree<M> additions = new HashMappingTree<>();
	private MappingTree<M> deletions = new HashMappingTree<>();

	public DeltaTrackingTree(MappingTree<M> delegate) {
		this.delegate = delegate;
	}

	public DeltaTrackingTree() {
		this(new HashMappingTree<>());
	}

	@Override
	public void insert(Entry<?> entry, M mapping) {
		if (mapping != null) {
			trackAddition(entry, mapping);
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

	private void trackAddition(Entry<?> entry, M mapping) {
		deletions.remove(entry);
		additions.insert(entry, mapping);
	}

	private void trackDeletion(Entry<?> entry) {
		additions.remove(entry);

		M previousMapping = delegate.getMapping(entry);
		deletions.insert(entry, previousMapping);
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
	public Collection<Entry<?>> getAllEntries() {
		return delegate.getAllEntries();
	}

	@Override
	public Iterator<MappingNode<M>> iterator() {
		return delegate.iterator();
	}

	public MappingDelta<M> takeDelta() {
		MappingDelta<M> delta = new MappingDelta<>(additions, deletions);
		resetDelta();
		return delta;
	}

	private void resetDelta() {
		additions = new HashMappingTree<>();
		deletions = new HashMappingTree<>();
	}
}
