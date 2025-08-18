package cuchaz.enigma.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cuchaz.enigma.api.Ordering;

public sealed interface OrderingImpl extends Ordering {
	static List<String> sort(String serviceName, Map<String, List<Ordering>> idToOrderings) {
		List<String> first = new ArrayList<>();
		List<String> middle = new ArrayList<>();
		Map<String, Integer> inDegree = new HashMap<>();
		Map<String, Set<String>> graph = new HashMap<>();

		idToOrderings.forEach((id, orderings) -> {
			inDegree.put(id, 0);
			graph.put(id, new LinkedHashSet<>());

			boolean isFirst = false;
			boolean isLast = false;
			for (Ordering ordering : orderings) {
				if (ordering instanceof First) {
					isFirst = true;
				} else if (ordering instanceof Last) {
					isLast = true;
				}
			}

			if (isFirst) {
				if (isLast) {
					throw new IllegalArgumentException("Service " + id + " has both 'first' and 'last' ordering");
				}

				first.add(id);
			} else if (!isLast) {
				middle.add(id);
			}
		});

		idToOrderings.forEach((id, orderings) -> {
			boolean isFirst = false;
			boolean isLast = false;

			for (Ordering ordering : orderings) {
				if (ordering instanceof Before before) {
					if (idToOrderings.containsKey(before.id())) {
						if (graph.get(id).add(before.id())) {
							inDegree.merge(before.id(), 1, Integer::sum);
						}
					}
				} else if (ordering instanceof After after) {
					if (idToOrderings.containsKey(after.id())) {
						if (graph.get(after.id()).add(id)) {
							inDegree.merge(id, 1, Integer::sum);
						}
					}
				} else if (ordering instanceof First) {
					isFirst = true;
				} else if (ordering instanceof Last) {
					isLast = true;
				}
			}

			if (!isFirst) {
				for (String aFirst : first) {
					if (graph.get(aFirst).add(id)) {
						inDegree.merge(id, 1, Integer::sum);
					}
				}
				if (isLast) {
					for (String aMiddle : middle) {
						if (graph.get(aMiddle).add(id)) {
							inDegree.merge(id, 1, Integer::sum);
						}
					}
				}
			}
		});

		Deque<String> queue = new ArrayDeque<>();
		for (String id : idToOrderings.keySet()) {
			if (inDegree.get(id) == 0) {
				queue.add(id);
			}
		}

		List<String> result = new ArrayList<>(idToOrderings.size());

		while (!queue.isEmpty()) {
			String id = queue.remove();
			result.add(id);

			for (String successor : graph.get(id)) {
				if (inDegree.merge(successor, -1, Integer::sum) == 0) {
					queue.add(successor);
				}
			}
		}

		if (result.size() != idToOrderings.size()) {
			throw new IllegalStateException("Services in " + serviceName + " contain circular dependencies");
		}

		return result;
	}

	enum First implements OrderingImpl {
		INSTANCE
	}

	enum Last implements OrderingImpl {
		INSTANCE
	}

	record Before(String id) implements OrderingImpl {
	}

	record After(String id) implements OrderingImpl {
	}
}
