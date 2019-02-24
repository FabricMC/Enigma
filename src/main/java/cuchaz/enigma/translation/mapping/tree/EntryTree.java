package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Stream;

public interface EntryTree<T> extends EntryMap<T>, Iterable<EntryTreeNode<T>>, Translatable {
	Collection<Entry<?>> getChildren(Entry<?> entry);

	@Nullable
	EntryTreeNode<T> findNode(Entry<?> entry);

	Stream<EntryTreeNode<T>> getRootNodes();

	@Override
	EntryTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);
}
