package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.representation.entry.Entry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

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

	default Collection<Entry<?>> getChildrenRecursively() {
		return getNodesRecursively().stream()
				.map(EntryTreeNode::getEntry)
				.collect(Collectors.toList());
	}

	default boolean hasValue() {
		return getValue() != null;
	}
}
