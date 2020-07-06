package cuchaz.enigma.gui.stats;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatsResult {

	private final int total;
	private final int unmapped;
	private final Tree<Integer> tree;

	public StatsResult(int total, int unmapped, Tree<Integer> tree) {
		this.total = total;
		this.unmapped = unmapped;
		this.tree = tree;
	}

	public int getTotal() {
		return total;
	}

	public int getUnmapped() {
		return unmapped;
	}

	public int getMapped() {
		return total - unmapped;
	}

	public double getPercentage() {
		return (getMapped() * 100.0f) / total;
	}

	public String getTreeJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(tree.root);
	}

	@Override
	public String toString() {
		return String.format("%s/%s %.1f%%", getMapped(), total, getPercentage());
	}

	public static class Tree<T> {
		public final Node<T> root;
		private final Map<String, Node<T>> nodes = new HashMap<>();

		public static class Node<T> {
			public String name;
			public T value;
			public List<Node<T>> children = new ArrayList<>();
			private final transient Map<String, Node<T>> namedChildren = new HashMap<>();

			public Node(String name, T value) {
				this.name = name;
				this.value = value;
			}
		}

		public Tree() {
			root = new Node<>("", null);
		}

		public Node<T> getNode(String name) {
			Node<T> node = nodes.get(name);

			if (node == null) {
				node = root;

				for (String part : name.split("\\.")) {
					Node<T> child = node.namedChildren.get(part);

					if (child == null) {
						child = new Node<>(part, null);
						node.namedChildren.put(part, child);
						node.children.add(child);
					}

					node = child;
				}

				nodes.put(name, node);
			}

			return node;
		}

		public void collapse(Node<T> node) {
			while (node.children.size() == 1) {
				Node<T> child = node.children.get(0);
				node.name = node.name.isEmpty() ? child.name : node.name + "." + child.name;
				node.children = child.children;
				node.value = child.value;
			}

			for (Node<T> child : node.children) {
				collapse(child);
			}
		}
	}

}
