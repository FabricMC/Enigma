package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.*;

public class HashMappingTree<M> implements MappingTree<M> {
	private final Map<Entry, MappingNode<M>> root = new HashMap<>();

	@Override
	public void insert(Entry entry, M mapping) {
		List<MappingNode<M>> path = computePath(entry);
		path.get(path.size() - 1).putMapping(mapping);
		if (mapping == null) {
			removeDeadAlong(path);
		}
	}

	@Override
	public void remove(Entry entry) {
		List<MappingNode<M>> path = computePath(entry);
		path.get(path.size() - 1).removeMapping();

		removeDeadAlong(path);
	}

	@Override
	@Nullable
	public M getMapping(Entry entry) {
		MappingNode<M> node = leaf(entry);
		if (node == null) {
			return null;
		}
		return node.getMapping();
	}

	@Override
	public boolean hasMapping(Entry entry) {
		return getMapping(entry) != null;
	}

	@Override
	public Collection<Entry> getChildren(Entry entry) {
		MappingNode<M> leaf = leaf(entry);
		if (leaf == null) {
			return Collections.emptyList();
		}
		return leaf.getChildren();
	}

	@Override
	public Collection<Entry> getSiblings(Entry entry) {
		List<MappingNode<M>> path = computePath(entry);
		if (path.size() <= 1) {
			return getSiblings(entry, root.keySet());
		}
		MappingNode<M> parent = path.get(path.size() - 2);
		return getSiblings(entry, parent.getChildren());
	}

	private Collection<Entry> getSiblings(Entry entry, Collection<Entry> children) {
		Set<Entry> siblings = new HashSet<>(children);
		siblings.remove(entry);
		return siblings;
	}

	@Override
	@Nullable
	public MappingNode<M> leaf(Entry target) {
		List<Entry> parentChain = target.getAncestry();
		if (parentChain.isEmpty()) {
			return null;
		}

		MappingNode<M> node = root.get(parentChain.get(0));
		for (int i = 1; i < parentChain.size(); i++) {
			if (node == null) {
				return null;
			}
			node = node.child(parentChain.get(i));
		}

		return node;
	}

	private List<MappingNode<M>> computePath(Entry target) {
		List<Entry> ancestry = target.getAncestry();
		if (ancestry.isEmpty()) {
			return Collections.emptyList();
		}

		List<MappingNode<M>> path = new ArrayList<>(ancestry.size());

		Entry rootEntry = ancestry.get(0);
		MappingNode<M> node = root.computeIfAbsent(rootEntry, MappingNode::new);
		path.add(node);

		for (int i = 1; i < ancestry.size(); i++) {
			node = node.child(ancestry.get(i));
			path.add(node);
		}

		return path;
	}

	private void removeDeadAlong(List<MappingNode<M>> path) {
		for (int i = path.size() - 1; i > 0; i--) {
			MappingNode<M> node = path.get(i);
			if (node.isEmpty()) {
				MappingNode<M> parentNode = path.get(i - 1);
				parentNode.remove(node.getEntry());
			} else {
				break;
			}
		}
	}

	@Override
	public Iterator<MappingNode<M>> iterator() {
		return root.values().iterator();
	}

	@Override
	public Collection<Entry> getEntries() {
		Collection<Entry> entries = new ArrayList<>();
		for (MappingNode<M> node : root.values()) {
			entries.addAll(node.collectEntries());
		}
		return entries;
	}
}
