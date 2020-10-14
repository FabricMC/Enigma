package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.Entry;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class HashEntryTree<T> implements EntryTree<T> {
	private final Map<Entry<?>, HashTreeNode<T>> root = new HashMap<>();

	public HashEntryTree() {
	}

	public HashEntryTree(EntryTree<T> tree) {
		for (EntryTreeNode<T> node : tree) {
			insert(node.getEntry(), node.getValue());
		}
	}

	@Override
	public void insert(Entry<?> entry, T value) {
		List<HashTreeNode<T>> path = computePath(entry, true);
		path.get(path.size() - 1).putValue(value);
		if (value == null) {
			removeDeadAlong(path);
		}
	}

	@Override
	@Nullable
	public T remove(Entry<?> entry) {
		List<HashTreeNode<T>> path = computePath(entry, false);
		if (path.isEmpty()) {
			return null;
		}

		T value = path.get(path.size() - 1).removeValue();

		removeDeadAlong(path);

		return value;
	}

	@Override
	@Nullable
	public T get(Entry<?> entry) {
		HashTreeNode<T> node = findNode(entry);
		if (node == null) {
			return null;
		}
		return node.getValue();
	}

	@Override
	public boolean contains(Entry<?> entry) {
		return get(entry) != null;
	}

	@Override
	public Collection<Entry<?>> getChildren(Entry<?> entry) {
		HashTreeNode<T> leaf = findNode(entry);
		if (leaf == null) {
			return Collections.emptyList();
		}
		return leaf.getChildren();
	}

	@Override
	public Collection<Entry<?>> getSiblings(Entry<?> entry) {
		Entry<?> parent = entry.getParent();
		if (parent == null) {
			return getSiblings(entry, root.keySet());
		}
		return getSiblings(entry, getChildren(parent));
	}

	private Collection<Entry<?>> getSiblings(Entry<?> entry, Collection<Entry<?>> generation) {
		Set<Entry<?>> siblings = new HashSet<>(generation);
		siblings.remove(entry);
		return siblings;
	}

	@Override
	@Nullable
	public HashTreeNode<T> findNode(Entry<?> target) {
		List<Entry<?>> parentChain = target.getAncestry();
		if (parentChain.isEmpty()) {
			return null;
		}

		HashTreeNode<T> node = root.get(parentChain.get(0));
		for (int i = 1; i < parentChain.size(); i++) {
			if (node == null) {
				return null;
			}
			node = node.getChild(parentChain.get(i));
		}

		return node;
	}

	private List<HashTreeNode<T>> computePath(Entry<?> target, boolean make) {
		List<Entry<?>> ancestry = target.getAncestry();
		if (ancestry.isEmpty()) {
			return Collections.emptyList();
		}

		List<HashTreeNode<T>> path = new ArrayList<>(ancestry.size());

		Entry<?> rootEntry = ancestry.get(0);
		HashTreeNode<T> node = make ? root.computeIfAbsent(rootEntry, HashTreeNode::new) : root.get(rootEntry);
		if (node == null) {
			return Collections.emptyList();
		}

		path.add(node);

		for (int i = 1; i < ancestry.size(); i++) {
			Entry<?> ancestor = ancestry.get(i);
			node = make ? node.computeChild(ancestor) : node.getChild(ancestor);
			if (node == null) {
				return Collections.emptyList();
			}

			path.add(node);
		}

		return path;
	}

	private void removeDeadAlong(List<HashTreeNode<T>> path) {
		for (int i = path.size() - 1; i >= 0; i--) {
			HashTreeNode<T> node = path.get(i);
			if (node.isEmpty()) {
				if (i > 0) {
					HashTreeNode<T> parentNode = path.get(i - 1);
					parentNode.remove(node.getEntry());
				} else {
					root.remove(node.getEntry());
				}
			} else {
				break;
			}
		}
	}

	@Override
	public Iterator<EntryTreeNode<T>> iterator() {
		Collection<EntryTreeNode<T>> nodes = new ArrayList<>();
		for (EntryTreeNode<T> node : root.values()) {
			nodes.addAll(node.getNodesRecursively());
		}
		return nodes.iterator();
	}

	@Override
	public Stream<Entry<?>> getAllEntries() {
		return StreamSupport.stream(spliterator(), false)
				.filter(EntryTreeNode::hasValue)
				.map(EntryTreeNode::getEntry);
	}

	@Override
	public Stream<EntryTreeNode<T>> getRootNodes() {
		return root.values().stream().map(Function.identity());
	}

	@Override
	public boolean isEmpty() {
		return root.isEmpty();
	}

	@Override
	public HashEntryTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		HashEntryTree<T> translatedTree = new HashEntryTree<>();
		for (EntryTreeNode<T> node : this) {
			translatedTree.insert(translator.translate(node.getEntry()), node.getValue());
		}
		return translatedTree;
	}
}
