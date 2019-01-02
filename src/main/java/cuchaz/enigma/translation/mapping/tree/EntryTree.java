package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;

public interface EntryTree<T> extends EntryMap<T>, Iterable<HashTreeNode<T>> {
	Collection<Entry<?>> getChildren(Entry<?> entry);

	Collection<Entry<?>> getSiblings(Entry<?> entry);

	@Nullable
	HashTreeNode<T> findNode(Entry<?> entry);

	Collection<HashTreeNode<T>> getAllNodes();

	Collection<Entry<?>> getRootEntries();
}
