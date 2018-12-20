package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.mapping.MappingSet;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;

public interface MappingTree<M> extends MappingSet<M>, Iterable<MappingNode<M>> {
	Collection<Entry<?>> getChildren(Entry<?> entry);

	Collection<Entry<?>> getSiblings(Entry<?> entry);

	@Nullable
	MappingNode<M> findNode(Entry<?> entry);

	Collection<Entry<?>> getRootEntries();
}
