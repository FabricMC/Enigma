package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class HashTreeNode<T> implements Iterable<HashTreeNode<T>> {
	private final Entry<?> entry;
	private final Map<Entry<?>, HashTreeNode<T>> children = new HashMap<>();
	private T value;

	HashTreeNode(Entry<?> entry) {
		this.entry = entry;
	}

	void putValue(T value) {
		this.value = value;
	}

	void removeValue() {
		this.value = null;
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

	@Nullable
	public T getValue() {
		return value;
	}

	public Entry<?> getEntry() {
		return entry;
	}

	public boolean isEmpty() {
		return children.isEmpty() && value == null;
	}

	public Collection<Entry<?>> getChildren() {
		return children.keySet();
	}

	public Collection<HashTreeNode<T>> getChildNodes() {
		return children.values();
	}

	@Override
	public Iterator<HashTreeNode<T>> iterator() {
		return children.values().iterator();
	}

	public Collection<HashTreeNode<T>> getNodesRecursively() {
		Collection<HashTreeNode<T>> nodes = new ArrayList<>();
		nodes.add(this);
		for (HashTreeNode<T> node : children.values()) {
			nodes.addAll(node.getNodesRecursively());
		}
		return nodes;
	}

	public Collection<Entry<?>> getChildrenRecursively() {
		return getNodesRecursively().stream()
				.map(HashTreeNode::getEntry)
				.collect(Collectors.toList());
	}
}
