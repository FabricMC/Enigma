package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.*;

public class MappingNode<M> implements Iterable<MappingNode<M>> {
	private final Entry<?> entry;
	private final Map<Entry<?>, MappingNode<M>> children = new HashMap<>();
	private M mapping;

	MappingNode(Entry<?> entry) {
		this.entry = entry;
	}

	void putMapping(M mapping) {
		this.mapping = mapping;
	}

	void removeMapping() {
		this.mapping = null;
	}

	MappingNode<M> getChild(Entry<?> entry, boolean create) {
		if (create) {
			return children.computeIfAbsent(entry, MappingNode::new);
		} else {
			return children.get(entry);
		}
	}

	void remove(Entry<?> entry) {
		children.remove(entry);
	}

	@Nullable
	public M getMapping() {
		return mapping;
	}

	public Entry<?> getEntry() {
		return entry;
	}

	public boolean isEmpty() {
		return children.isEmpty() && mapping == null;
	}

	public Collection<Entry<?>> getChildren() {
		return children.keySet();
	}

	public Collection<MappingNode<M>> getChildNodes() {
		return children.values();
	}

	@Override
	public Iterator<MappingNode<M>> iterator() {
		return children.values().iterator();
	}

	public Collection<MappingNode<M>> getNodesRecursively() {
		Collection<MappingNode<M>> nodes = new ArrayList<>();
		nodes.add(this);
		for (MappingNode<M> node : children.values()) {
			nodes.addAll(node.getNodesRecursively());
		}
		return nodes;
	}
}
