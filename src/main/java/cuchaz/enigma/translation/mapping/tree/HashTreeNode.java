package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HashTreeNode<T> implements EntryTreeNode<T>, Iterable<HashTreeNode<T>> {
	private final Entry<?> entry;
	private final Map<Entry<?>, HashTreeNode<T>> children = new HashMap<>();
	private T value;

	HashTreeNode(Entry<?> entry) {
		this.entry = entry;
	}

	void putValue(T value) {
		this.value = value;
	}

	T removeValue() {
		T value = this.value;
		this.value = null;
		return value;
	}

	HashTreeNode<T> getChild(Entry<?> entry, boolean create) {
		if (create) {
			return children.computeIfAbsent(entry, HashTreeNode::new);
		} else {
			return children.get(entry);
		}
	}

	void remove(Entry<?> entry) {
		children.remove(entry);
	}

	@Override
	@Nullable
	public T getValue() {
		return value;
	}

	@Override
	public Entry<?> getEntry() {
		return entry;
	}

	@Override
	public boolean isEmpty() {
		return children.isEmpty() && value == null;
	}

	@Override
	public Collection<Entry<?>> getChildren() {
		return children.keySet();
	}

	@Override
	public Collection<? extends EntryTreeNode<T>> getChildNodes() {
		return children.values();
	}

	@Override
	public Iterator<HashTreeNode<T>> iterator() {
		return children.values().iterator();
	}
}
