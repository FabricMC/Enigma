package cuchaz.enigma.translation.mapping.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import cuchaz.enigma.translation.representation.entry.Entry;

public interface EntryTreeNode<T> {
	@Nullable
	T getValue();

	Entry<?> getEntry();

	boolean isEmpty();

	Collection<Entry<?>> getChildren();

	Collection<? extends EntryTreeNode<T>> getChildNodes();

	default Collection<? extends EntryTreeNode<T>> getNodesRecursively() {
		Collection<EntryTreeNode<T>> nodes = new ArrayList<>();
		nodes.add(this);

		for (EntryTreeNode<T> node : getChildNodes()) {
			nodes.addAll(node.getNodesRecursively());
		}

		return nodes;
	}

	default List<? extends Entry<?>> getChildrenRecursively() {
		return getNodesRecursively().stream().map(EntryTreeNode::getEntry).toList();
	}

	default boolean hasValue() {
		return getValue() != null;
	}
}
