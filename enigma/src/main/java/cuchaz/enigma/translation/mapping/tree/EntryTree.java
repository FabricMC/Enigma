package cuchaz.enigma.translation.mapping.tree;

import java.util.Collection;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface EntryTree<T> extends EntryMap<T>, Iterable<EntryTreeNode<T>>, Translatable {
	Collection<Entry<?>> getChildren(Entry<?> entry);

	Collection<Entry<?>> getSiblings(Entry<?> entry);

	@Nullable
	EntryTreeNode<T> findNode(Entry<?> entry);

	Stream<EntryTreeNode<T>> getRootNodes();

	@Override
	default TranslateResult<? extends EntryTree<T>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return TranslateResult.ungrouped(this.translate(translator, resolver, mappings));
	}

	@Override
	EntryTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);
}
